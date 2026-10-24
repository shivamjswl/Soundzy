package app.cadence;

import android.app.Activity;

/** The unconfigured build keeps music/history features usable without Google SDKs. */
public final class GoogleAccount {
    public interface Callback {void signedIn(String name,String email);void failed(String message);}
    public GoogleAccount(Activity activity){}
    public boolean configured(){return false;}
    public void signIn(Callback callback){callback.failed("Google sign-in isn't configured for this build. You can import history without signing in.");}
    public void signOut(Runnable done){done.run();}
    public void cancel(){}
}
