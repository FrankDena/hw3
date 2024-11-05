import java.util.Scanner;

public class lucenex {
    public static void main(String[] args) {
        // Prints "Hello, World" in the terminal window.
        Scanner sc= new Scanner(System.in); //System.in is a standard input stream
        System.out.print("Enter a string: ");
        String str= sc.nextLine();              //reads string
        System.out.print("You have entered: "+str);
    }
}
