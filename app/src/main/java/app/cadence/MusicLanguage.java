package app.cadence;

import android.content.Context;
import java.util.Locale;

/** A discovery preference, not a claim that an upload's audio language is verified. */
public enum MusicLanguage {
    ANY("All languages"), ENGLISH("English"), HINDI("Hindi"), PUNJABI("Punjabi"), TAMIL("Tamil"), TELUGU("Telugu"), BENGALI("Bengali"), MARATHI("Marathi"), GUJARATI("Gujarati"), KANNADA("Kannada"), MALAYALAM("Malayalam"), URDU("Urdu"), SPANISH("Spanish"), KOREAN("Korean"), JAPANESE("Japanese"), ARABIC("Arabic"), FRENCH("French");
    public final String label;
    MusicLanguage(String label){this.label=label;}
    public static MusicLanguage selected(Context context){try{return valueOf(context.getSharedPreferences("musedock.preferences",0).getString("language","ANY"));}catch(IllegalArgumentException e){return ANY;}}
    public void save(Context context){context.getSharedPreferences("musedock.preferences",0).edit().putString("language",name()).apply();}
    public String query(String value){String q=value.trim();if(this==ANY||VideoIds.parse(q)!=null||PlaylistLinks.canonical(q)!=null)return q;
        if(java.util.regex.Pattern.compile("(?iu)(?<!\\p{L})"+java.util.regex.Pattern.quote(label)+"(?!\\p{L})").matcher(q).find())return q;
        return label+" "+q;
    }
}
