//
// Simple initial ASM class intrumentor example
//
// Idea: write a visitor that passes on everything it visits
// to a ClassWriter, but then also "visit" extra stuff that adds
// the extra stuff (instrumentation code) into what it writes out.
//
// - the ClassWriter inherits from ClassVisitor, so is a visitor,
//   but it builds an internal representation of a new class as it
//   visits the old class
// - the ClassWriter gives us a MethodVisitor that is its method writer,
//   so this also builds a new method in the new class that is the same
//   as the old method, EXCEPT, that we add three instructions at the
//   top that print out "Tracer". We do this by telling the writer to
//   "visit" these three instructions, and in doing so it creates them
//   in the new class
//
import org.objectweb.asm.*;
import java.io.FileOutputStream;

public class ClassInstrumentor extends ClassVisitor {

// Create our custom method instrumentation class
// - it does the instrumentation by telling the class
//   writer object to "visit" new instructions, which adds
//   them to the method being created
class CodeInstrumentor extends MethodVisitor {
   // our writer object
   MethodVisitor mwriter;
   // constructor
   CodeInstrumentor(MethodVisitor mwriter){
      super(Opcodes.ASM9,mwriter);
      this.mwriter = mwriter;
   }
   // method instrumentor
   public void visitCode() {
      // tell the writer to visit all the code
      mwriter.visitCode();
      //mwriter.visitInsn(Opcodes.NOP);  // add a NOP into the bytecode
      // We figured out the code below by using "javap -c" on real code
      // 13: getstatic  #26  // Field java/lang/System.out:Ljava/io/PrintStream;
      // 16: ldc        #28  // String Hello
      // 18: invokevirtual #34  // Method java/io/PrintStream.println:
      //                                        (Ljava/lang/String;)V
      // now add these three instructions to the top of the method
      mwriter.visitFieldInsn(Opcodes.GETSTATIC,"java/lang/System", "out",
                             "Ljava/io/PrintStream;");
      mwriter.visitLdcInsn("--Begin Method--"); // TODO: add this method's name
      mwriter.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream", 
                              "println", "(Ljava/lang/String;)V",false);
      // after visitCode(), all the instructions are visited in the original
      // method, so these get added to the top.
   }
   // put a trace on end of method -- to do this we have to watch for
   // return instructions, and put our trace instructions above it
   public void visitInsn(int opcode) {
      if (opcode == Opcodes.ARETURN || opcode == Opcodes.DRETURN ||
          opcode == Opcodes.FRETURN || opcode == Opcodes.IRETURN ||
          opcode == Opcodes.LRETURN || opcode == Opcodes.RETURN) {
         mwriter.visitFieldInsn(Opcodes.GETSTATIC,"java/lang/System", "out",
                                "Ljava/io/PrintStream;");
         mwriter.visitLdcInsn("--End Method--"); // TODO: add this method's name
         mwriter.visitMethodInsn(Opcodes.INVOKEVIRTUAL,"java/io/PrintStream", 
                                 "println", "(Ljava/lang/String;)V",false);
      }
      mwriter.visitInsn(opcode);
   }
} // end class CodeInstrumentor

ClassVisitor writer; // the ClassWriter object
int myField; // just a test field; not used

// Constructor
public ClassInstrumentor(ClassVisitor writer) {
   super(Opcodes.ASM9,writer);
   this.writer = writer;
   System.out.println("Class instrumentor");
}

// Method visitor (this is where we start the instrumentation)
public MethodVisitor visitMethod(int access, String name, String desc,
                                 String signature, String[] exceptions) 
{
   System.out.println("Method: " + name + desc);
   // create a method writer from the class writer (this is all
   // using built-in ASM classes and interfaces
   MethodVisitor mwriter = writer.visitMethod(access, name, desc,
                                              signature, exceptions);
   // now create our own code instrumentor object to connect it all up
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
   // create a class writer object
   ClassWriter cw = new ClassWriter(0);
   // pass the writer to our class visitor object
   ClassInstrumentor ci = new ClassInstrumentor(cw);
   try {
      ClassReader cr = new ClassReader(classname);
      cr.accept(ci, 0);
   } catch (Exception e) {
      System.err.println("Class read error: " + e);
   }
   try {
      // TODO: create a better name for the output class file
      FileOutputStream outf = new FileOutputStream("t.class");
      outf.write(cw.toByteArray());
      outf.close();
   } catch (Exception e) {
      System.err.println("Class write error: " + e);
   }
} // end main

}

