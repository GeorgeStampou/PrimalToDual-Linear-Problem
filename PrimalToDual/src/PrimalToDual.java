import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrimalToDual {

	public static void main(String[] args) {
		
		
		ArrayList<String> data = new ArrayList<String>();
		ArrayList<String> dataWithoutBlanks = new ArrayList<String>();
		ArrayList<String> functions = new ArrayList<String>();
		String dual = null;
		String lastLine = null;
		List<String> dataWithoutLastLine = new ArrayList<String>();
		List<List<Double>> A = new ArrayList<List<Double>>();
		List<List<Double>> transposedA = new ArrayList<List<Double>>();
		List<Double> b = new ArrayList<Double>();
		List<Double> c = new ArrayList<Double>();
		List<Integer> Eqin = new ArrayList<Integer>();
		List<String> constraints = new ArrayList<String>();
		List<Integer> indexes = new ArrayList<Integer>();
		List<String> dualVariables = new ArrayList<String>();
		List<String> dEqin = new ArrayList<String>();
		String inputFile;
		String outputFile;
		String variable = "";
		String function = "";
		int maxIndex;
		
		Scanner scan = new Scanner(System.in);
		
		System.out.println("Input file :");
		inputFile = scan.nextLine();
		
		try {
			File f = new File(inputFile);
			FileReader freader = new FileReader(f);
			BufferedReader reader = new BufferedReader(freader);
			
			String line  = reader.readLine();
			data.add(line);
			while(line != null) {
				
				line = reader.readLine();
				data.add(line);
				
			}
			
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*System.out.println("Output File :");
		outputFile = scan.nextLine();
		
		File fout = new File(outputFile);*/
		
		dataWithoutBlanks = removeBlanksAndEmptyLines(data);
		
		lastLine = dataWithoutBlanks.get(dataWithoutBlanks.size()-1);
		
		functions = removeMinMaxSt(dataWithoutBlanks);
		dataWithoutLastLine = functions.subList(0, functions.size()-1);
		constraints = dataWithoutLastLine.subList(1, dataWithoutLastLine.size());
		
		//εύρεση της μεταβλητής της συνάρτησης
		if(checkProblemVariables(functions)) {
			variable = getLineVariable(functions.get(0));
		}
		else
			System.out.println("Problem with the variables. Use the same character at all lines");
		
		function = String.join(" ", dataWithoutLastLine);
		indexes = getVariableIndexes(function,variable);
		maxIndex = Collections.max(indexes);
		
		for(String line: constraints) {
			Eqin.add(checkAndGetSymbols(line)); 
		}
		
		for(String line: constraints){
			List<Double> linefactors = new ArrayList<Double>();
			linefactors = getFactors(line,variable,maxIndex);
			A.add(linefactors);
		}
		
		int minMax = getMinMax(data.get(0));
		
		if(minMax == -1) {
			System.out.print("max ");
			//appendStrToFile(fout,"max ");
		}
		else if(minMax == 1) {
			System.out.print("min ");
			//appendStrToFile(fout,"min ");
		}
		
	
		
		for(String line :constraints)
			b.add(checkAndGetB(line));
		
		
		dual = functionsToDual(b);
		//appendStrToFile(fout,dual);
		System.out.println(dual);
		
		System.out.println(A);
		
		if(checkSt(data.get(1))) {
			System.out.print("s.t.");
			//appendStrToFile(fout,"\n");
			//appendStrToFile(fout,"s.t.");
		}
		
		transposedA = transposeA(A);
		c = getFactors(dataWithoutLastLine.get(0),variable,maxIndex);
		dEqin = dualEqin(lastLine,minMax,maxIndex);
		
		int i =0;
		for(List<Double> lineData: transposedA) {
			String ad = functionsToDual(lineData);
			System.out.println(ad+dEqin.get(i)+c.get(i));
			i++;
			if(i> c.size())
				System.out.println(ad+dEqin.get(i)+"0.0");
		}
		
		
		System.out.println(transposedA);
		
		dualVariables = variablesToDual(Eqin,minMax);
		System.out.println(dualVariables);
		
		
		System.out.println(dEqin);
		
		scan.close();
		

	}
	
	public static String functionsToDual(List<Double> data) {
		
		ArrayList<String> dualFunction = new ArrayList<String>();
		
		int index =1;
		for(Double number: data) {
			if(number.equals(0.0)) {
				index ++;
			}
			else {
				if(!(number.toString().contains("-") || number.toString().contains("+")) && index>=2)
					dualFunction.add("+"+number.toString()+"w"+Integer.toString(index));
				else
					dualFunction.add(number.toString()+"w"+Integer.toString(index));
				index ++;
			}
		}
		
		String dual = String.join(", ", dualFunction);
		dual = dual.replaceAll(",", "");
		
		
		
		return dual;
	}
	
	public static List<List<Double>>  transposeA(List<List<Double>> A) {
		
		List<List<Double>> Anew = new ArrayList<List<Double>>();
		
		int start = 0;
		for(int j=0; j<A.get(0).size();j++) {
			List<Double> d = new ArrayList<Double>();
			for(int i=0; i< A.size();i++) {
				d.add(A.get(i).get(j));
			}
			start ++;
			Anew.add(d);
		}
		
		return Anew;
	}
	
	public static List<String> variablesToDual(List<Integer> Eqin,int minMax){
		
		List<String> variables = new ArrayList<String>();
		int index = 1;
		if(minMax == -1) { //min
			for(int i: Eqin) {
				if(Eqin.get(i) == -1) { //<=
					variables.add("w"+index+" <= 0 ");
					index ++;
				}
				else if(Eqin.get(i) == 0) { //=
					variables.add("w"+index+" free");
					index ++;
				}
				else if(Eqin.get(i) == 1) { // >=
					variables.add("w"+index+" >=0 ");
					index ++;
				}
			}
			
		}
		else if(minMax ==1) { //max
			for(int i=0; i<Eqin.size();i++) {
				if(Eqin.get(i) == -1) { //<=
					variables.add("w"+index+" >= 0");
					index ++;
				}
				else if(Eqin.get(i) == 0) { //=
					variables.add("w"+index+" ελευθερη");
					index ++;
				}
				else if(Eqin.get(i) == 1) { // >=
					variables.add("w"+index+" <=0");
					index ++;
				}
			}
			
			
		}
		
		
		return variables;
	}
	
	public static List<String> dualEqin (String lastLine, int minMax, int maxIndex){
		
		List<String> newEqin = new ArrayList<String>();
		//lastLine = lastLine.replaceAll("\\D", "");
		
		
		if(minMax ==-1) {
			
			for(int i =1; i<=maxIndex; i++) {
				if(lastLine.contains(Integer.toString(i))) {
					if(lastLine.contains("free") || lastLine.contains("ελευθερη")) {
						newEqin.add(" = ");
						lastLine = lastLine.replaceFirst("free", "");
						lastLine = lastLine.replaceFirst("ελευθερη", "");
					}
					else newEqin.add(" <= ");
				}
				else {
					newEqin.add(" = ");
				}
			}
			
			
		}
		else if(minMax == 1) {
			
			for(int i =1; i<=maxIndex; i++) {
				if(lastLine.contains(Integer.toString(i))) {
					if(lastLine.contains("free") || lastLine.contains("ελευθερη")) {
						newEqin.add(" = ");
						lastLine = lastLine.replaceFirst("free", "");
						lastLine = lastLine.replaceFirst("ελευθερη", "");
					}
					else newEqin.add(" >= ");	
				}
				else {
					newEqin.add(" = ");
				}
			}
			
			
		}
		
		return newEqin;
	}
	
	
	public static ArrayList<String> removeBlanksAndEmptyLines(ArrayList<String> data) {
		
		ArrayList<String> dWithoutBlanks = new ArrayList<String>();
		
		for(int i=0; i<data.size()-1;i++) {
			String a = data.get(i);
			a = a.replaceAll("\\s", ""); //αφαίρεση κενών
			a = a.toLowerCase();
			dWithoutBlanks.add(a);
			
		}
		dWithoutBlanks.removeAll(Collections.singletonList("")); //αφαίρεση κενων γραμμών
		
		//δημιουργια και εισαγωγή σε set για να αφαιρεθούν ιδιες γραμμές με συναρτήσεις
		Set<String> dataSet = new LinkedHashSet<String>(dWithoutBlanks); 
		dWithoutBlanks.clear();
		dWithoutBlanks.addAll(dataSet);
		
		return dWithoutBlanks;
	}
	
	public static int getMinMax(String line) {
		
		if(line.startsWith("min"))
			return -1;
		else if(line.startsWith("max"))
			return 1;
		else {
			System.out.println("Problem with min/max. Check again.");
			System.exit(0);
			return 0;
		}
		
	}
	
	public static boolean checkSt(String line) {
		
		if(line.startsWith("st") || line.startsWith("s.t.") || line.startsWith("subject to"))
			return true;
		else {
			System.out.println("Problem with st/s.t./subject to. Check again.");
			System.exit(0);
			return false;
		}
		
	}
	
	public static ArrayList<String> removeMinMaxSt(ArrayList<String> data){
		
		String line = null;
		ArrayList<String> dFunctions = new ArrayList<String>();
		
		//ελεγχος πρωτης γραμμης
		line = data.get(0);
		line = line.replaceAll("max", "").replaceAll("min", "");
		dFunctions.add(line);
		
		//έλεγχος δεύτερης γραμμής
		line = data.get(1);
		line = line.replaceAll("st", "").replaceAll("s.t.", "").replaceAll("subjectto","");
		dFunctions.add(line);
		
		for(int j =2; j<data.size();j++) {
			dFunctions.add(data.get(j));
		}
		
		return dFunctions;
		
	}
	public static String getLineVariable(String line) {
		
		String simplifiedLine = null;
		boolean mistake = false;
		simplifiedLine = line.replaceAll("\\W", "").replaceAll("\\d", "");//αφαίρεση αριθμητικών χαρακτήρων και χαρακτήρων που δεν ειναι γράμματα
		//έλεγχος μεταβλητών (αν συμβολίζονται με τον ιδιο χαρακτήρα)
		for(int j = 0 ; j<simplifiedLine.length();j++) {
			if(simplifiedLine.charAt(j) != simplifiedLine.charAt(0)) {
				mistake = true;
			}
		
		}
		
		if(mistake) {
			System.out.println("You do not use the same characters for variables in line: "+line);
			System.exit(0);
		}
		return String.valueOf(simplifiedLine.charAt(0));
	}
	
		
	
	public static boolean checkProblemVariables(ArrayList<String> functions) {
		
		String lastLine = null;
		//δημιουργια set ώστε να ελέγχεται αν οι μεταβλητές είναι ίδιες
		Set<String> variables = new HashSet<String>();
		for(int i=0; i<functions.size()-1; i++) {
			
			variables.add(getLineVariable(functions.get(i)));
		}
		//αν χρησιμοποιείται ο ίδιος χαρακτήρας και στην τελευταία γραμμή και αν το set 
		//έχει έναν χαρακτήρα μέσα τότε έχουμε την ίδια μεταβλητή στο πρόβλημα
		lastLine = functions.get(functions.size()-1);
		if(variables.contains(String.valueOf(lastLine.charAt(0)))){
			if(variables.size()==1)
				return true;
				
		}
		
		return false;
			
	}
	
	
	public static List<Integer> getVariableIndexes(String data,String variable) {
		
		List<Integer> index = new ArrayList<Integer>();
		Pattern p = Pattern.compile(variable+"[0-9]+|"+variable);//δημιουργια μοτίβου της μορφής "μεταβλητη+αριθμοι" ή μόνο μεταβλητή
		Matcher m = p.matcher(data);
		String part = "";
		
		
		while(m.find()) {
			part = m.group();
			part = part.replaceAll(variable, "");//αφαίρεση του χαρακτήρα της μεταβλητής 
			//αν το part είναι άδειο τοτε σημαίνει οτι υπήρχε μεταβλητή χωρίς δείκτη
			if(part.isEmpty()) {
				System.out.println("An index of a variable missing! Check again.");
				System.exit(0);
			}
			else
				index.add(Integer.parseInt(part));
			
			
		}
		
		
		return index;
		
	}
	
	public static void checkOperands(String line,String variable) {
		
		
		
		line = line.replaceAll("\\d", "").replaceAll("[<=>==]", "");//αφαίρεση αριθμητικών χαρακτηρων και συμβόλων που φαίνονται στις [ ]
		//αφαίρεση τελειών για τους δεκαδικούς, αν υπάρχουν
		if(line.contains("."))
			line = line.replaceAll("\\.", "");
		
		//αν ξεκινάει η γραμμή με + ή - ελέγχει τον δεύτερο χαρακτήρα για να δει αν είναι μεταβλητή
		if(line.startsWith("-")||line.startsWith("+")) {
			if(line.charAt(1) != variable.charAt(0)) {
				System.out.println("Problem with operands. Check agian.");
				System.exit(0);
		
			}
			line = line.replaceFirst("[+-]", "");
		}
		
		
		//ελέγχει όλους τους χαρακτήρες της γραμμής. Μόλις βρει τον χαρακτήρα της μεταβλητής
		//βλέπει αν ο προηγούμενος είναι + ή - καθώς οι συντελεστές εχουν φύγει
		for(int i = 1; i<line.length(); i++) {
			if(line.charAt(i) == variable.charAt(0)) {
				if((int)line.charAt(i-1) != 43 && (int)line.charAt(i-1) != 45) {
					System.out.println("Problem with operands. Check again.");
					System.exit(0);
				}
			}
		}
		
	}
	
	public static int checkAndGetSymbols(String line) {
		
		int symbol = 2;
		
		line = line.replaceAll("\\d", "").replaceAll("[+-.]", "");//αφαίρεση αριθμητικών χαρακτηρων και συμβόλων [ ]
		if(line.endsWith("<="))
			symbol =-1;
		else if(line.endsWith(">="))
			symbol =  1;
		else if(line.endsWith("="))
			symbol =  0;
		else if(symbol == 2){
			System.out.println("Problem with symbols(<= , = , >=). Check again.");
			System.exit(0);
		}
		
		return symbol;
		
		
	}
	
	public static Double checkAndGetB(String line) {
		
		String result ="";
		
		Pattern p = Pattern.compile("[-][0-9]+$|[+][0-9]+$|[0-9]+$");//δημιουργία μοτίβου : αριθμοί στο τέλος της γραμμής
		Matcher m = p.matcher(line);
		if(m.find()) {
		    result = m.group();
		}
		else {
			System.out.println("There are no numbers after one of these symbols(<=,>=,=). Check again.");
			System.exit(0);
		}
		
		return Double.parseDouble(result);	
		
	}
	
	
	public static List<Double> getFactors(String line,String variable,int maxIndex){
		
		List<Double> numbers = new ArrayList<Double>();
		String part = "";

		//for μέχρι τον μέγιστο δείκτη ώστε να έχουν τον ίδιο αριθμό στοιχείων οι λίστες του Α
		for(int i =1 ; i<= maxIndex; i++) {
			
			//δημιουργια μοτίβου για ολες τις πιθανές καταστάσεις μεταβλητών
			//-xj,+xj,-ixj, +ixj, ixj , xj, +- δεκαδικος xj
			Pattern p = Pattern.compile("[-]"+variable+Integer.toString(i)+"| [+]"+variable+Integer.toString(i)+"|[-+][0-9]+"+variable+Integer.toString(i)+"|[0-9]+"+variable+Integer.toString(i)+"|"+variable+Integer.toString(i)+"|[+-][0-9]+[\\.\\,][0-9]+"+variable+Integer.toString(i));
			Matcher m = p.matcher(line);
				
			//αν υπάρχει κάποιο απο τα μοτίβα
			if(m.find()) {
				part = m.group();
				
				//βάζει 1 αν υπάρχει +xj ή x 
				if(part.equals("+"+variable+Integer.toString(i)) || part.equals(variable+Integer.toString(i))) {
					numbers.add(1.0);
				}
				//βάζει -1 αν υπάρχει -xj
				else if(part.contentEquals("-"+variable+Integer.toString(i))) {
					numbers.add(-1.0);
				}
				//βάζει τον αριθμό που υπάρχει στο part 
				else {
					String mpart = part.replaceAll(variable+Integer.toString(i), "");
					numbers.add(Double.parseDouble(mpart));
				}
			}
			//αν δεν υπάρχει κάποιο από τα μοτίβα σημαίνει οτι το xi δεν υπάρχει στην συνάρτηση
			//και βάζει 0
			else
				numbers.add(0.0);
						
		}
		
		return numbers;
	}
	
	
	public static void appendStrToFile(File file, String str) { 
		
		try { 

				// Open given file in append mode. 
				BufferedWriter out = new BufferedWriter( new FileWriter(file, true)); 
				out.write(str); 
				out.close(); 
		} 
		catch (IOException e) { 
			e.printStackTrace();
		} 
	} 	
}
