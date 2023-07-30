package util;

public class Arg {

    public final String srcFile;
    public final String targetFile;
    public static boolean opt = false;

    public Arg (String srcFile,String targetFile){
        this.srcFile =srcFile;
        this.targetFile = targetFile;
    }
    public static Arg parse(String[] args){
        String src = "";
        String target = "";

        for(int i = 0; i < args.length; i++){

            if(args[i].startsWith("-O")){
                int optLevel = Integer.parseInt(args[i].substring(2));
                if(optLevel == 1){
                    opt = true;
                }
            }

            if(args[i].equals("-S")){
                if(i + 2 < args.length && args[i + 1].equals("-o")){
                    target = args[i + 2];
                }else{
                    throw new RuntimeException("-S expected -o filename");
                }
                i += 2;
                continue;
            }
            if (!src.isEmpty()) {
                throw new RuntimeException("We got more than one source file when we expected only one.");
            }
            src = args[i];
        }
        if(src.isEmpty()){
            printHelp();
            throw new RuntimeException("source file should be specified.");
        }
        Arg arg = new Arg(src,target);
        return arg;
    }

    public static void printHelp() {
        System.err.println("Usage: compiler -S -o filename filename");
    }
}
