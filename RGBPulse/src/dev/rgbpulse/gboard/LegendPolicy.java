package dev.rgbpulse.gboard;
/** Conservative legend coverage. Never change emoji, icon, combining or shaping runs. */
final class LegendPolicy {
    static boolean eligible(String text){
        if(text==null||text.isEmpty()||text.length()>24)return false;
        boolean visible=false;
        for(int i=0;i<text.length();){
            int cp=text.codePointAt(i);i+=Character.charCount(cp);
            if(cp==' ')continue;
            boolean digit=cp>='0'&&cp<='9';
            boolean latin=Character.isLetter(cp)&&Character.UnicodeScript.of(cp)==Character.UnicodeScript.LATIN;
            if(!digit&&!latin)return false;
            visible=true;
        }return visible;
    }
}
