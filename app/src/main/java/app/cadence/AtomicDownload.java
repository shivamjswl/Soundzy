package app.cadence;

import java.io.*;

public final class AtomicDownload {
    private AtomicDownload(){}
    public interface Progress { void update(long bytes,long total) throws IOException; }
    public static void save(InputStream input,File target,long total,Progress progress) throws IOException {
        File part=new File(target.getParentFile(),target.getName()+".part");
        try{
            long count=0;
            try(FileOutputStream out=new FileOutputStream(part)){
                byte[] buffer=new byte[64*1024];int n;
                while((n=input.read(buffer))!=-1){out.write(buffer,0,n);count+=n;progress.update(count,total);}
                out.getFD().sync();
            }
            if(count==0)throw new IOException("The server returned an empty audio file.");
            if(total>=0 && total!=count)throw new IOException("Download was incomplete. Please retry.");
            java.nio.file.Files.move(part.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING,java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        }finally{if(part.exists()&&!part.delete())part.deleteOnExit();}
    }
}
