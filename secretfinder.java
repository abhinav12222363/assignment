import java.io.*;
import java.math.BigInteger;
import java.util.*;

public class SecretFinder {
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader("input.json"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line.trim());
        String json = sb.toString();

        int n = extractInt(json, "\"n\"");
        int k = extractInt(json, "\"k\"");

        Map<Integer, BigInteger> xList = new LinkedHashMap<>();
        Map<Integer, BigInteger> yList = new LinkedHashMap<>();

        for (int i = 1; i <= n; i++) {
            String key = "\"" + i + "\"";
            int keyIndex = json.indexOf(key);
            if (keyIndex == -1) continue;

            int baseIndex = json.indexOf("\"base\"", keyIndex);
            int baseStart = json.indexOf("\"", baseIndex + 7) + 1;
            int baseEnd = json.indexOf("\"", baseStart);
            int base = Integer.parseInt(json.substring(baseStart, baseEnd));
            if (base < 2 || base > 36) continue;

            int valueIndex = json.indexOf("\"value\"", baseEnd);
            int valueStart = json.indexOf("\"", valueIndex + 8) + 1;
            int valueEnd = json.indexOf("\"", valueStart);
            String valueStr = json.substring(valueStart, valueEnd);

            BigInteger x = BigInteger.valueOf(i);
            BigInteger y = new BigInteger(valueStr, base);

            xList.put(i, x);
            yList.put(i, y);
        }

        List<BigInteger> xs = new ArrayList<>(xList.values());
        List<BigInteger> ys = new ArrayList<>(yList.values());
        List<Integer> keys = new ArrayList<>(xList.keySet());

        BigInteger bestSecret = null;
        int bestFitCount = -1;
        int[] bestSubset = null;

        List<int[]> subsets = new ArrayList<>();
        generateSubsets(0, xs.size(), k, new int[k], 0, subsets);

        for (int[] subset : subsets) {
            List<BigInteger> subXs = new ArrayList<>();
            List<BigInteger> subYs = new ArrayList<>();
            for (int idx : subset) {
                subXs.add(xs.get(idx));
                subYs.add(ys.get(idx));
            }
            BigInteger secret = lagrangeAtZero(subXs, subYs);

            int fitCount = 0;
            for (int i = 0; i < xs.size(); i++) {
                BigInteger calcY = lagrangeEvaluate(xs.get(i), subXs, subYs);
                if (calcY.equals(ys.get(i))) fitCount++;
            }

            if (fitCount > bestFitCount) {
                bestFitCount = fitCount;
                bestSecret = secret;
                bestSubset = subset.clone();
            }
        }

        System.out.println("Correct secret (constant term c): " + bestSecret);
        System.out.println("Shares fitting polynomial: " + bestFitCount + " out of " + xs.size());

        // Identify wrong shares: those NOT in bestSubset OR don't fit polynomial
        Set<Integer> subsetIndices = new HashSet<>();
        for (int idx : bestSubset) subsetIndices.add(idx);

        System.out.print("Wrong shares (x values): ");
        boolean first = true;
        for (int i = 0; i < xs.size(); i++) {
            BigInteger calcY = lagrangeEvaluate(xs.get(i),
                    getSubList(xs, bestSubset),
                    getSubList(ys, bestSubset));
            if (!calcY.equals(ys.get(i))) {
                if (!first) System.out.print(", ");
                System.out.print(keys.get(i));
                first = false;
            }
        }
        System.out.println();
    }

    private static List<BigInteger> getSubList(List<BigInteger> list, int[] indices) {
        List<BigInteger> sublist = new ArrayList<>();
        for (int idx : indices) sublist.add(list.get(idx));
        return sublist;
    }

    static void generateSubsets(int start, int n, int k, int[] subset, int idx, List<int[]> results) {
        if (idx == k) {
            results.add(subset.clone());
            return;
        }
        for (int i = start; i <= n - (k - idx); i++) {
            subset[idx] = i;
            generateSubsets(i + 1, n, k, subset, idx + 1, results);
        }
    }

    static BigInteger lagrangeAtZero(List<BigInteger> xs, List<BigInteger> ys) {
        BigInteger secret = BigInteger.ZERO;
        int k = xs.size();
        for (int i = 0; i < k; i++) {
            BigInteger xi = xs.get(i);
            BigInteger yi = ys.get(i);
            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;
            for (int j = 0; j < k; j++) {
                if (j == i) continue;
                BigInteger xj = xs.get(j);
                num = num.multiply(xj.negate());
                den = den.multiply(xi.subtract(xj));
            }
            secret = secret.add(yi.multiply(num).divide(den));
        }
        return secret;
    }

    static BigInteger lagrangeEvaluate(BigInteger x, List<BigInteger> xs, List<BigInteger> ys) {
        BigInteger result = BigInteger.ZERO;
        int k = xs.size();
        for (int i = 0; i < k; i++) {
            BigInteger xi = xs.get(i);
            BigInteger yi = ys.get(i);
            BigInteger num = BigInteger.ONE;
            BigInteger den = BigInteger.ONE;
            for (int j = 0; j < k; j++) {
                if (j == i) continue;
                BigInteger xj = xs.get(j);
                num = num.multiply(x.subtract(xj));
                den = den.multiply(xi.subtract(xj));
            }
            result = result.add(yi.multiply(num).divide(den));
        }
        return result;
    }

    private static int extractInt(String json, String key) {
        int index = json.indexOf(key);
        if (index == -1) return -1;
        int colon = json.indexOf(":", index);
        int start = colon + 1;
        int comma = json.indexOf(",", start);
        int brace = json.indexOf("}", start);
        int end = (comma == -1) ? brace : (brace == -1) ? comma : Math.min(comma, brace);

        String number = json.substring(start, end).replaceAll("[^0-9]", "");
        return Integer.parseInt(number);
    }
}
