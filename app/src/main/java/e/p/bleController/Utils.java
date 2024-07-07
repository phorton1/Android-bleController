package e.p.bleController;

import android.util.Log;
import android.widget.Toast;

public class Utils {

    public static int global_debug_level = 0;

    // common colors

    public static final int COLOR_BLACK   = 0x00000000;
    public static final int COLOR_WHITE   = 0xffffffff;
    public static final int COLOR_NORMAL  = 0xffaaaaaa;      // to blend in with white in holo-dark
    public static final int COLOR_GREY    = 0xff888888;      // grey lettering
    public static final int COLOR_GREEN   = 0xff009900;
    public static final int COLOR_RED     = 0xffcc0000;
    public static final int COLOR_CYAN    = 0xff00cccc;
    public static final int COLOR_MAGENTA = 0xffbb00bb;
    public static final int COLOR_YELLOW  = 0xffcccc00;
    public static final int COLOR_ORANGE  = 0xffdd7700;
    public static final int COLOR_TAN     = 0xffffbb88;  // liquidations


    //---------------------------------------
    // display routines
    //---------------------------------------

    public static void error(String msg)
    {
        log(0,-1,"ERROR: " + msg,1);
        if (true)   // show the error context
        {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            for (int level = 4; level < stack.length; level++)
            {
                StackTraceElement e = stack[level];
                Log.d("blec","... from " + e.getClassName() + "::" + e.getMethodName() + "(" + e.getFileName() + ":" + e.getLineNumber() + ")");

                // optional .. only show one level past our package

                if (true && !e.getClassName().startsWith("e.p")) { break; }
            }
        }

        // show a toast

        MainActivity.showToast(msg);

    }    // error()



    public static void warning(int debug_level,int indent_level,String msg)
    {
        log(debug_level,indent_level,"WARNING: " + msg,1);
    }

    public static void log(int debug_level,int indent_level,String msg)
    {
        log(debug_level,indent_level,msg,1);
    }


    public static void log(int debug_level,int indent_level,String msg,int call_level)
    {

        if (debug_level <= global_debug_level)
        {
            // The debugging filter is by java filename
            // get the incremental level due to the call stack

            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            int level = 0;
            while (level + call_level + 4 < stack.length &&
                    stack[level + call_level + 4].getClassName().startsWith("e.p"))
            {
                level++;
            }
            // Log.d("blec","--- level ends up as " + level);
            if (indent_level >= 0)
            {
                indent_level += level;
            }

            int use_level = call_level + 3;
            if (use_level >= stack.length)
                use_level = stack.length-1;

            StackTraceElement caller = stack[use_level];
            String filename = caller.getFileName();
            // filename = filename.replaceAll("\\.java$", "");

            String indent = "";
            while (indent_level-- > 0)
            {
                indent += "   ";
            }

            Log.d("blec",pad("(" + filename + ":" + caller.getLineNumber() + ")",27) + " " + indent + msg);
        }
    }




    public static void display_bytes(int dbg, int level, String msg, byte[] bytes)
    {
        String hex = "";
        String ascii = "";
        int offset = 0;

        log(dbg,level,msg,1);
        for (int i = 0; i < bytes.length; i++)
        {
            byte b = bytes[i];
            char c = (char) b;
            if (b < 32) c = '.';
            ascii += c;
            hex += String.format("x%02X ",b);

            if ((i+1) % 16 == 0)
            {
                log(dbg,level + 1,
                        String.format("%06x",offset) + " " +
                                pad("(" + offset + ")",10) + " " +
                                hex + "    " + ascii,1);
                offset += 16;
                hex = "";
                ascii = "";
            }
        }
        if (!hex.isEmpty())
            log(dbg,level + 1,
                    String.format("%06x",offset) + " " +
                            pad("(" + offset + ")",10) + " " +
                            hex + "    " + ascii,1);
    }


    //--------------------------------------
    // string routines
    //--------------------------------------


    public static String pad(String in,int len)
    {
        String out = in;
        while (out.length() < len) {out = out + " ";}
        return out;
    }

    public static String pad2(String in)
    {
        if (in.length() < 2) in = "0" + in;
        return in;
    }



    public static int parseInt(String s)
    {
        int retval = 0;
        if (s != null && !s.equals(""))
        {
            try
            {
                s = s.replaceAll("\\.*","");
                retval = Integer.valueOf(s);
            }
            catch (Exception e)
            {
                warning(0,0,"parseInt(" + s + ") exception:" + e.toString());
            }
        }
        return retval;
    }

    public static float parseFloat(String s)
    {
        float retval = 0;
        if (s != null && !s.equals(""))
        {
            try
            {
                retval = Float.parseFloat(s);
            }
            catch (Exception e)
            {
                warning(0,0,"parseFloat(" + s + ") exception:" + e.toString());
            }
        }
        return retval;
    }



}
