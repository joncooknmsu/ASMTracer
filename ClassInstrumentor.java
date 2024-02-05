//
// Simple initial ASM class intrumentor example
//
// Idea: write a visitor that passes on everything it visits
// to a ClassWriter, but then also "visit" extra stuff that adds
// the extra stuff (instrumentation code) into what it writes out
// -- the ClassWriter inherits from ClassVisitor, so is a visitor
// -- the ClassWriter gives us a MethodVisitor that is its method writer
//
import org.objectweb.asm.*;
import java.io.FileOutputStream;

public class ClassInstrumentor extends ClassVisitor {

ClassVisitor writer;

class CodeInstrumentor extends MethodVisitor {
   MethodVisitor mwriter;
   CodeInstrumentor(MethodVisitor mwriter){
      super(Opcodes.ASM9,mwriter);
      this.mwriter = mwriter;
   }
   public void visitCode() {
      mwriter.visitCode();
      //mwriter.visitInsn(Opcodes.NOP);  // add a NOP into the bytecode
      // Code below is for a simple string printout
      // 13: getstatic  #26  // Field java/lang/System.out:Ljava/io/PrintStream;
      // 16: ldc        #28  // String Hello
      // 18: invokevirtual #34  // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      mwriter.visitFieldInsn(Opcodes.GETSTATIC,"java/lang/System", "out",
                             "Ljava/io/PrintStream;");
      mwriter.visitLdcInsn("Tracer");
      mwriter.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream", 
                              "println", "(Ljava/lang/String;)V",false);
   }
}

int myField;

public ClassInstrumentor(ClassVisitor writer) {
   super(Opcodes.ASM9,writer);
   this.writer = writer;
   System.out.println("Hello");
}

public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) 
{
   System.out.println("" + name + desc);
   MethodVisitor mwriter = writer.visitMethod(access, name, desc, signature, exceptions);
   MethodVisitor mv = new CodeInstrumentor(mwriter);
   return mv;
}

//public void visitEnd() {
//   System.out.println("}");
//}

public static void main(String args[])
{
   String classname;
   if (args==null || args.length==0 || args[0]==null)
      classname = "java.lang.Runnable";
   else
      classname = args[0];
   ClassWriter cw = new ClassWriter(0);
   ClassInstrumentor cp = new ClassInstrumentor(cw);
   try {
      ClassReader cr = new ClassReader(classname);
      cr.accept(cp, 0);
   } catch (Exception e) {
      System.err.println("Error: " + e);
   }
   try {
      FileOutputStream outf = new FileOutputStream("t.class");
      outf.write(cw.toByteArray());
      outf.close();
   } catch (Exception e) {
   }
}
}

