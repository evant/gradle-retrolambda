package me.tatarka

class RetrolambdaUtil {
    static String capitalize(CharSequence self) {
        return self.length() == 0 ? "" : "" + Character.toUpperCase(self.charAt(0)) + self.subSequence(1, self.length())
    }
}
