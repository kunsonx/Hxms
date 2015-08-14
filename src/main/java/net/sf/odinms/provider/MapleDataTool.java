
package net.sf.odinms.provider;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ListIterator;
import java.util.Stack;

import net.sf.odinms.provider.wz.MapleDataType;

public class MapleDataTool {
    
    public static String getString(MapleData data) {
        return ((String) data.getData());
    }

    public static String getString(MapleData data, String def) {
        if (data == null || data.getData() == null) {
            return def;
        } else {
            if (data.getType() == MapleDataType.INT) {
                return String.valueOf(getInt(data));
            } else {
                return ((String) data.getData());
            }
        }
    }

    public static String getString(String path, MapleData data) {
        return getString(data.getChildByPath(path));
    }

    public static String getString(String path, MapleData data, String def) {
        return getString(data.getChildByPath(path), def);
    }

    public static double getDouble(MapleData data) {
        return ((Double) data.getData()).doubleValue();
    }

    public static float getFloat(MapleData data) {
        return ((Float) data.getData()).floatValue();
    }

    public static int getInt(MapleData data) {
        return ((Integer) data.getData()).intValue();
    }

    public static int getInt(MapleData data, int def) {
        if (data == null || data.getData() == null) {
            return def;
        } else if (data.getType() == MapleDataType.STRING) {
            return Integer.parseInt(getString(data));
        } else {
            return ((Integer) data.getData()).intValue();
        }
    }

    public static int getInt(String path, MapleData data) {
        return getInt(data.getChildByPath(path));
    }

    public static int getIntConvert(MapleData data) {
        if (data.getType() == MapleDataType.STRING) {
            return Integer.parseInt(getString(data));
        } else {
            return getInt(data);
        }
    }

    public static int getIntConvert(String path, MapleData data) {
        MapleData d = data.getChildByPath(path);
        if (d.getType() == MapleDataType.STRING) {
            return Integer.parseInt(getString(d));
        } else {
            return getInt(d);
        }
    }

    public static int getInt(String path, MapleData data, int def) {
        return getInt(data.getChildByPath(path), def);
    }

    public static int getIntConvert(String path, MapleData data, int def) {
        MapleData d = data.getChildByPath(path);
        if (d == null) {
            return def;
        }
        if (d.getType() == MapleDataType.STRING) {
            try {
                return Integer.parseInt(getString(d));
            } catch (NumberFormatException nfe) {
                return def;
            }
        } else {
            return getInt(d, def);
        }
    }

    public static BufferedImage getImage(MapleData data) {
        return ((MapleCanvas) data.getData()).getImage();
    }

    public static Point getPoint(MapleData data) {
        return ((Point) data.getData());
    }

    public static Point getPoint(String path, MapleData data) {
        return getPoint(data.getChildByPath(path));
    }

    public static Point getPoint(String path, MapleData data, Point def) {
        final MapleData pointData = data.getChildByPath(path);
        if (pointData == null) {
            return def;
        }
        return getPoint(pointData);
    }

    public static String getFullDataPath(MapleData data) {
        String path = "";
        MapleDataEntity myData = data;
        while (myData != null) {
            path = myData.getName() + "/" + path;
            myData = myData.getParent();
        }
        return path.substring(0, path.length() - 1);
    }

    public static int getIntFromBigBang(String path, MapleData data, int defalut, int level){
        String temp = getString(path,data,"");
        int ro = 50000;  //50000做不会溢出吧。
        int num = 0;
        int num2 =0;
        if(temp.toString().equals(""))
            return defalut;
        for(int j =0;j < temp.length();j++)
            if(temp.substring(j, j+1).toString().equals("+") || temp.substring(j, j+1).toString().equals("-") || temp.substring(j, j+1).toString().equals("*") || temp.substring(j, j+1).toString().equals("/"))
                num2 ++;
        if(temp.substring(0, 1).toString().equals("-"))
            for(int i =0;i < temp.length();i++)
                if(temp.substring(i, i+1).toString().equals("+") || temp.substring(i, i+1).toString().equals("-") || temp.substring(i, i+1).toString().equals("*") || temp.substring(i, i+1).toString().equals("/"))
                    num ++;
        if(num == 1)
            return Integer.parseInt(temp);
        else if(num > 1) 
            temp = String.valueOf(ro) + temp;
        for(int i =0;i < temp.length();i++)
            if(temp.substring(i, i+1).toString().equals("d"))
                temp = temp.substring(0,i) +"1*"+ temp.substring(i+1, temp.length());
        for(int i =0;i < temp.length();i++)
            if(temp.substring(i, i+1).toString().equals("u"))
                temp = temp.substring(0,i) +"1*("+ temp.substring(i+1, temp.length())+"+1)";
        for(int i =0;i < temp.length();i++)
            if(temp.substring(i, i+1).toString().equals("x"))
                temp = temp.substring(0,i) +level+ temp.substring(i+1, temp.length());
        if(num2 == 0)
            return Integer.parseInt(temp);
        int db = Integer.MAX_VALUE;
        db = (int) new DBloader(temp).getResult();
        if(num > 1){
            db = ro - db;
            db = db - db * 2;
        }
        if(db == Integer.MAX_VALUE)
            return Integer.parseInt(temp);
        return db;
    }

    private static class DBloader {



        private String src;

        /**
          *   constructor
          *
          *   @param   srcthe   string(expression)   to   calculate
          */
        public  DBloader(String src)   {
            this.src  = src;
        }

        /**
          *   calculate   to   get   the   result
          *
          *   @return(double)result
          */
        public double getResult(){
                String postfix = getPostfix();
                Stack<String> stk  =new Stack<String>();
                //   log.debug(postfix);
                String parts[] = postfix.split(" +");
                double result = 0;
                for   (int i =0;i < parts.length;i++){
                        char tmp = parts[i].charAt(0);
                        if (!isOperator(tmp)){
                                stk.push(parts[i]);
                        }   else   {
                                double a = Double.parseDouble((String) stk.pop());
                                double b = Double.parseDouble((String) stk.pop());
                                //   b is followed by a in the orignal expression
                                result = calculate(b,a,tmp);
                                stk.push(String.valueOf(result));
                        }
                }
                return   result;
        }

        /**
          *   test   if   the   character   is   an   operator,such   +,-,*,/
          *
          *   @param   opthe   character   to   test
          *   @returntrue   if   op   is   an   operator   otherwise   false
          */
        private   boolean   isOperator(char op)   {
                return   (op   ==   '+'   ||   op   ==   '-'   ||   op   ==   '*'   ||   op   ==   '/');
        }

        /**
          *   calculate   an   expression   such   (a   op   b)
          *
          *   @param   anumber   1
          *   @param   bnumber   2
          *   @param   opthe   operator
          *   @return(double)(a   op   b)
          */
        public   double   calculate(double   a,   double   b,   char   op)   {
                switch   (op)   {
                case   '+':
                        return   a   +   b;
                case   '-':
                        return   a   -   b;
                case   '*':
                        return   a   *   b;
                case   '/':
                        return   a   /   b;
                }
                return   -1;
        }

        /**
          *   convert   the   suffix   to   postfix
          *
          *   @returnthe   postfix   as   a   string
          */
        private   String   getPostfix()   {
                Stack<String>   stk   =   new   Stack<String>();
                String   postfix   =   new   String();
                char   op;
                int   i   =   0;
                while   (i   <  src.length())   {
                        if   (Character.isDigit(src.charAt(i))  ||   src.charAt(i)   ==   '.') {
                                postfix   +=   " ";
                                do   {
                                    postfix  += src.charAt(i++);
                                }   while   ((i   <   src.length()) &&(Character.isDigit(src.charAt(i))));
                                postfix   +=   " ";
                        }

                        else   {
                                switch (op   =   src.charAt(i++))   {
                                case   '(':
                                        stk.push( "(");
                                        break;

                                case   ')':
                                        while   (stk.peek()   !=   "(")   {
                                                String   tmp   =   (String)   stk.pop();
                                                postfix   +=   tmp;
                                                if   (tmp.length()   ==   1   &&   isOperator(tmp.charAt(0)))
                                                        postfix   +=   " ";
                                        }
                                        stk.pop();
                                        postfix   +=   " ";
                                        break;

                                case   '+':
                                case   '-':
                                        while   ((!stk.empty())   &&   (stk.peek()   !=   "("))   {
                                                postfix   +=   stk.pop()   +   " ";
                                        }
                                        stk.push(String.valueOf(new   Character(op)));
                                        break;

                                case   '*':
                                case   '/':
                                        while   ((!stk.empty())
                                                        &&   ((stk.peek()   ==   "*")   ||   (stk.peek()   ==   "/")))   {
                                                postfix   +=   stk.pop()   +   " ";
                                        }
                                        stk.push(String.valueOf(new   Character(op)));
                                        break;
                                }
                        }
                }
                ListIterator<String>   it   =   stk.listIterator(stk.size());
                while   (it.hasPrevious())
                        postfix   +=   it.previous()   +   " ";
                return   postfix.trim().replaceAll( " +\\.",   ".");
        }

        /**
          *   main   function
          *
          *   @param   args
          */
        public static void  main(String   args[])   {
                System.out.println(new DBloader("100 -10-1*5").getResult());
                System.out.println(new DBloader("100 -85.0").getResult());

        }
    }
}

/* Location:           C:\Documents and Settings\Administrator\桌面\AbomsDev.jar
 * Qualified Name:     provider.MapleDataTool
 * JD-Core Version:    0.5.4
 */