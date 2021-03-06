package Classifier.SVM;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketPermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class Classifier
{
	HashSet< String > stopwords = new HashSet< String >();
	LinkedHashMap< Integer, String > classNameMap = new LinkedHashMap< Integer, String >();
	ArrayList< String > uniqueWords = new ArrayList< String >();
	ArrayList< TrainData > trainDataList = new ArrayList< TrainData >();
	LinkedHashMap< String, Integer > testClassNameMap = new LinkedHashMap< String, Integer >();
	ArrayList< TrainData > testDataList = new ArrayList< TrainData >();

	final String STOPWORD_FILENAME = "stopwords";
	final String OUTPUT_TRAIN_FILE = "train.txt";
	final String OUTPUT_TEST_FILE = "test.txt";
	final String OUTPUT_MODEL_FILE = "model.txt";
	final String OUTPUT_RESULT_FILE = "result.txt";
	final String UNIQUE_FILENAME = "unique_words.txt";

    public static String inst;

	protected void buildTrainData( String class_filename, String training_folder ) throws IOException
	{
		getStopWords();
		readClassNames( class_filename );
		readTrainFiles( training_folder );
		writeTrainFile();
		writeUniqueWords();

	}

	protected void buildTestFile( String test_class_filename, String testing_folder ) throws IOException
	{
		getStopWords();
		getUniqueWords();
		readTestClassNames( test_class_filename );
		readTestFiles( testing_folder );
		writeTestFile();
	}

	public String writeResultFile() throws IOException
	{
		BufferedReader br = new BufferedReader( new FileReader( OUTPUT_RESULT_FILE ) );
		String line;
		int i = 0;
		StringBuilder sb = new StringBuilder();

		while ( (line = br.readLine()) != null )
		{
			if ( !line.isEmpty() )
				sb.append( testDataList.get( i++ ).filename ).append( " " + (line.split( "\\." ))[ 0 ] ).append( "\n" );
		}
		br.close();

		File file = new File( OUTPUT_RESULT_FILE );
		file.createNewFile();

		FileWriter fw = new FileWriter( file.getAbsoluteFile() );
		BufferedWriter bw = new BufferedWriter( fw );
		bw.write( sb.toString() );
		bw.close();
        System.out.println("Finished writing output result file - " + OUTPUT_RESULT_FILE);
		return "Finished writing output result file - " + OUTPUT_RESULT_FILE;
	}

	public String getStopWords() throws IOException
	{
		BufferedReader br = new BufferedReader( new FileReader( STOPWORD_FILENAME ) );
		String line;

		while ( (line = br.readLine()) != null )
		{
			String[] tokens = line.split( "," );
			for ( String token : tokens )
				stopwords.add( token );
		}
		br.close();
        System.out.println("Finished reading stopwords.");
		return "Finished reading stopwords.";
	}

	private void getUniqueWords() throws IOException
	{
		BufferedReader br = new BufferedReader( new FileReader( UNIQUE_FILENAME ) );
		String line;

		while ( (line = br.readLine()) != null )
		{
			String[] tokens = line.split( "," );
			for ( String token : tokens )
				uniqueWords.add( token );
		}
		br.close();
	}

    private void readTestClassNames( String test_class_filename ) throws NumberFormatException, IOException
    {
        BufferedReader br = new BufferedReader( new FileReader( test_class_filename ) );
        String line;

        while ( (line = br.readLine()) != null )
        {
            String[] tokens = line.split( "\\s+" ); // Split words by space
            if ( tokens != null && tokens.length > 0 )
                testClassNameMap.put( tokens[ 0 ], Integer.parseInt( tokens[ 1 ] ) );
        }
        inst="Found " + testClassNameMap.size() + " test instances";
        System.out.println( "Found " + testClassNameMap.size() + " test instances" );

        br.close();
    }

	private String readClassNames( String class_filename ) throws IOException
	{
		BufferedReader br = new BufferedReader( new FileReader( class_filename ) );
		String line;

		while ( (line = br.readLine()) != null )
		{
			String[] tokens = line.split( "\\s+" ); // Split words by space
			if ( tokens != null && tokens.length > 0 )
				classNameMap.put( Integer.parseInt( tokens[ 0 ] ), tokens[ 1 ] );
		}
        br.close();
		return "Found " + classNameMap.size() + " classes";
	}


	private String readTestFiles( String testing_folder ) throws NumberFormatException, IOException
	{
		System.out.println( "Started reading test files" );
		Stemmer stemmer = new Stemmer();
		for ( Map.Entry< String, Integer > entry : testClassNameMap.entrySet() )
		{
			String testFileName = entry.getKey();
			File file = new File( testing_folder, testFileName );
			Scanner in = new Scanner( file );
			TrainData trainData = new TrainData( file.getName(), entry.getValue() );

			while ( in.hasNextLine() )
			{
				String[] tokens = in.nextLine().replaceAll( "[']", "" ).replaceAll( "[^a-zA-Z]", " " ).split( "\\s+" );

				for ( int i = 0; i < tokens.length; i++ )
				{
					String token = tokens[ i ].toLowerCase();
					token = stemmer.stem( token );
					if ( !token.isEmpty() && !stopwords.contains( token ) )
					{
						int index = getIndex( token );

						if ( trainData.nodeValueMap.containsKey( index ) )
						{
							double val = trainData.nodeValueMap.get( index );
							trainData.nodeValueMap.put( index, val++ );
						}
						else
							trainData.nodeValueMap.put( index, 1.0 );
					}
				}
			}
			testDataList.add( trainData );
			in.close();
		}
		return  "Finished reading test files";
	}


	private String readTrainFiles( String training_folder ) throws NumberFormatException, IOException
	{
		System.out.println( "Started reading class folder" );
		Stemmer stemmer = new Stemmer();
        String classFolderName ="";
		for ( Map.Entry< Integer, String > entry : classNameMap.entrySet() )
		{
			classFolderName = entry.getValue();
			File classFolder = new File( training_folder, classFolderName );
			File[] listofFiles = classFolder.listFiles();

			for ( File file : listofFiles )
			{
				Scanner in = new Scanner( file );
				TrainData trainData = new TrainData( file.getName(), entry.getKey() );

				while ( in.hasNextLine() )
				{
					String[] tokens = in.nextLine().replaceAll( "[']", "" ).replaceAll( "[^a-zA-Z]", " " ).split( "\\s+" );

					for ( int i = 0; i < tokens.length; i++ )
					{
						String token = tokens[ i ].toLowerCase();
						token = stemmer.stem( token );
						if ( !token.isEmpty() && !stopwords.contains( token ) )
						{
							int index = getIndex( token );

							if ( trainData.nodeValueMap.containsKey( index ) )
							{
								double val = trainData.nodeValueMap.get( index );
								trainData.nodeValueMap.put( index, val++ );
							}
							else
								trainData.nodeValueMap.put( index, 1.0 );
						}
					}
				}
				trainDataList.add( trainData );
				in.close();
			}
		}
        return "Finished reading folder " + classFolderName;
	}

	private String writeTrainFile() throws IOException
	{
		File file = new File( OUTPUT_TRAIN_FILE );
		file.createNewFile();

		FileWriter fw = new FileWriter( file.getAbsoluteFile() );
		BufferedWriter bw = new BufferedWriter( fw );


		for ( TrainData trainData : trainDataList )
		{
			bw.write( trainData.classLabel + "" );

			for ( Map.Entry< Integer, Double > entry : trainData.nodeValueMap.entrySet() )
				bw.write( " " + entry.getKey() + ":" + entry.getValue() );

			bw.write( "\n" );
		}
        bw.close();
		return "Finished writing training file - " + OUTPUT_TRAIN_FILE;
	}

	private String writeTestFile() throws IOException
	{
		File file = new File( OUTPUT_TEST_FILE );
		file.createNewFile();

		FileWriter fw = new FileWriter( file.getAbsoluteFile() );
		BufferedWriter bw = new BufferedWriter( fw );

		for ( TrainData trainData : testDataList )
		{
			bw.write( trainData.classLabel + "" );

			for ( Map.Entry< Integer, Double > entry : trainData.nodeValueMap.entrySet() )
				bw.write( " " + entry.getKey() + ":" + entry.getValue() );

			bw.write( "\n" );
		}
        bw.close();
		return "Finished writing test file - " + OUTPUT_TEST_FILE ;
	}

	private String writeUniqueWords() throws IOException
	{
		File file = new File( UNIQUE_FILENAME );
		file.createNewFile();

		FileWriter fw = new FileWriter( file.getAbsoluteFile() );
		BufferedWriter bw = new BufferedWriter( fw );

		for ( String words : uniqueWords )
			bw.write( words + "," );

        bw.close();
		return "Finished writing unique words file - " + UNIQUE_FILENAME ;
	}

	private int getIndex( String word )
	{
		if ( uniqueWords.contains( word ) )
			return (uniqueWords.indexOf( word ) + 1);
		else
		{
			uniqueWords.add( word );
			return uniqueWords.size();
		}
	}
}


class TrainData
{
	String filename;
	int classLabel;
	TreeMap< Integer, Double > nodeValueMap = new TreeMap< Integer, Double >();

	public TrainData ( String fileName, int classLabel )
	{
		this.filename = fileName;
		this.classLabel = classLabel;
	}
}
