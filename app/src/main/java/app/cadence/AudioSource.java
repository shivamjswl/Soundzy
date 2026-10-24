package app.cadence;

public record AudioSource(String url,String mime,String formatId,long length) {
    public String fingerprint(){return formatId+"|"+mime+"|"+length;}
    public String extension(){return mime.contains("webm")?"webm":mime.contains("mpeg")?"mp3":mime.contains("mp4")||mime.contains("m4a")?"m4a":"audio";}
}
