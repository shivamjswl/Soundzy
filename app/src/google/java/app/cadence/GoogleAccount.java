package app.cadence;

import android.app.Activity;
import android.os.CancellationSignal;
import androidx.credentials.*;
import androidx.credentials.exceptions.*;
import com.google.android.libraries.identity.googleid.*;

/** Optional Google identity only. The YouTube API does not expose watch history. */
public final class GoogleAccount {
    public interface Callback { void signedIn(String name,String email);void failed(String message); }
    private final Activity activity;
    private final CredentialManager credentials;
    private CancellationSignal cancellation;
    public GoogleAccount(Activity activity){this.activity=activity;credentials=CredentialManager.create(activity);}
    public boolean configured(){return !activity.getString(R.string.google_web_client_id).isBlank();}
    public void signIn(Callback callback){
        if(!configured()){callback.failed("Google sign-in is not configured for this build. History import works without signing in.");return;}
        cancellation=new CancellationSignal();
        GetSignInWithGoogleOption option=new GetSignInWithGoogleOption.Builder(activity.getString(R.string.google_web_client_id)).setNonce(java.util.UUID.randomUUID().toString()).build();
        credentials.getCredentialAsync(activity,new GetCredentialRequest.Builder().addCredentialOption(option).build(),cancellation,activity.getMainExecutor(),new CredentialManagerCallback<GetCredentialResponse,GetCredentialException>(){
            @Override public void onResult(GetCredentialResponse response){
                Credential value=response.getCredential();
                if(value instanceof CustomCredential custom&&GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(custom.getType())){
                    try{GoogleIdTokenCredential google=GoogleIdTokenCredential.createFrom(custom.getData());callback.signedIn(google.getDisplayName()==null||google.getDisplayName().isBlank()?google.getId():google.getDisplayName(),google.getId());}
                    catch(Exception e){callback.failed("Google returned an unsupported sign-in response.");}
                }else callback.failed("Google returned an unsupported sign-in method.");
            }
            @Override public void onError(GetCredentialException failure){if(!(failure instanceof GetCredentialCancellationException))callback.failed("Google sign-in couldn't finish. Check Google Play services and the app's OAuth setup.");}
        });
    }
    public void signOut(Runnable done){credentials.clearCredentialStateAsync(new ClearCredentialStateRequest(),null,activity.getMainExecutor(),new CredentialManagerCallback<Void,ClearCredentialException>(){
        @Override public void onResult(Void result){done.run();}
        @Override public void onError(ClearCredentialException failure){done.run();}
    });}
    public void cancel(){if(cancellation!=null)cancellation.cancel();}
}
