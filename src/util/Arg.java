package util;

public class Arg {

    public final String srcFile;
    public final String targetFile;
    public final String llvmFile;
    public static boolean opt = true;

    public Arg (String srcFile,String targetFile,String llvmFile){
        this.srcFile =srcFile;
        this.targetFile = targetFile;
        this.llvmFile = llvmFile;
    }
    public static Arg parse(String[] args){
        String src = "";
        String target = "";
        String llvm = "";

        for(int i = 0; i < args.length; i++){

            if(args[i].startsWith("-O")){
                int optLevel = Integer.parseInt(args[i].substring(2));
                if(optLevel == 1){
                    opt = true;
                }
                continue;
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

            if(args[i].equals("-emit-llvm")){
                if(i + 2 < args.length && args[i + 1].equals("-o")){
                    llvm = args[i + 2];
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
        Arg arg = new Arg(src,target,llvm);
        return arg;
    }

    public static void printHelp() {
        System.err.println("Usage: compiler -S -o filename filename");
    }
}
