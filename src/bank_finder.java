import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class BankFinder {

    private static final String FILE_URL =
            "https://ewib.nbp.pl/plewibnra?dokNazwa=plewibnra.txt";

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        Scanner sc = new Scanner(System.in);
        System.out.print("Enter the first three digits of the bank number: ");
        String digits = sc.nextLine().trim();

        if (!digits.matches("\\d{3}")) {
            System.out.println("Error: please enter exactly three digits.");
            return;
        }

        System.out.println("Loading data from NBP...");
        byte[] data;
        try (InputStream in = URI.create(FILE_URL).toURL().openStream()) {
            data = in.readAllBytes();
        } catch (IOException e) {
            System.out.println("Could not download the file, using local plewibnra.txt");
            try (InputStream in = new FileInputStream("plewibnra.txt")) {
                data = in.readAllBytes();
            } catch (IOException ex) {
                System.out.println("Error reading file: " + ex.getMessage());
                return;
            }
        }

        Map<String, String> banks = getBanks(decode(data));

        if (banks.containsKey(digits)) {
            System.out.println("Your account is in the bank: " + banks.get(digits));
        } else {
            System.out.println("No bank found with the number " + digits);
        }
    }

    private static String decode(byte[] data) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException e) {
            String best = null;
            int bestScore = -1;
            for (String cs : new String[]{"windows-1250", "IBM852", "ISO-8859-2"}) {
                String text = new String(data, Charset.forName(cs));
                int score = countPolishLetters(text);
                if (score > bestScore) {
                    bestScore = score;
                    best = text;
                }
            }
            return best;
        }
    }

    private static int countPolishLetters(String text) {
        int count = 0;
        for (char c : text.toCharArray()) {
            if ("ąćęłńóśźżĄĆĘŁŃÓŚŹŻ".indexOf(c) >= 0) {
                count++;
            }
        }
        return count;
    }

    private static Map<String, String> getBanks(String text) {
        Map<String, String> banks = new HashMap<>();
        for (String line : text.split("\\R")) {
            String[] columns = line.split("\t");
            if (columns.length < 2) {
                continue;
            }
            String number = columns[0].trim();
            String name = columns[1].trim();
            if (number.matches("\\d+") && !name.isEmpty() && !banks.containsKey(number)) {
                banks.put(number, name);
            }
        }
        return banks;
    }
}