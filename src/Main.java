

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.CannotRealizeException;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Port;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import marytts.modules.synthesis.Voice;

public class Main implements ActionListener
{
	
	JFrame jf;
	JLabel l1,l2,l3;
	Player p1;
	JButton b1;
	
	// Necessary
	EnglishNumberToString	numberToString	= new EnglishNumberToString();
	EnglishStringToNumber	stringToNumber	= new EnglishStringToNumber();
	TextToSpeech			textToSpeech	= new TextToSpeech();
	

	// Logger
	private Logger logger = Logger.getLogger(getClass().getName());

	// Variables
	private String result;

	// Threads
	Thread	speechThread;
	Thread	resourcesThread;

	// LiveRecognizer
	private LiveSpeechRecognizer recognizer;
	
	private volatile boolean recognizerStopped = true;

	/**
	 * Constructor
	 */
	public Main() 
	{

		jf = new JFrame("Jarvis");
		jf.setLayout(null);
		jf.setContentPane(new JLabel(new ImageIcon("img\\ai.gif")));
		

		
		jf.add(l1=new JLabel());
		l1.setBounds(100,30,700,40);
		l1.setFont(new Font("Times New Roman", Font.PLAIN, 35));
		l1.setForeground(Color.WHITE);
		
		jf.add(l2=new JLabel());
		l2.setBounds(100,100,600,40);
		l2.setFont(new Font("Times New Roman", Font.PLAIN, 35));
		l2.setForeground(Color.WHITE);
		
		jf.add(l3=new JLabel());
		l3.setBounds(300,300,600,40);
		l3.setFont(new Font("Times New Roman", Font.PLAIN, 35));
		l3.setForeground(Color.WHITE);
		
		jf.add(b1=new JButton("Pause"));
		b1.setBounds(100,500,200,40);
		b1.setFont(new Font("Times New Roman", Font.PLAIN, 35));
		b1.setForeground(Color.WHITE);
		b1.setBackground(Color.BLUE);
		
		// Loading Message
		logger.log(Level.INFO, "Loading..\n");

		// Configuration
		Configuration configuration = new Configuration();

		// Load model from the jar
		configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
		configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");

		// if you want to use LanguageModelPath disable the 3 lines after which
		// are setting a custom grammar->

		// configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin")

		// Grammar
		configuration.setGrammarPath("resource:/grammers");
		configuration.setGrammarName("grammar");
		configuration.setUseGrammar(true);

		try {
			recognizer = new LiveSpeechRecognizer(configuration);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		
		Voice.getAvailableVoices().stream().forEach(voice -> System.out.println("Voice: " + voice));
		textToSpeech.setVoice("cmu-slt-hsmm");
		//textToSpeech.setVoice("dfki-poppy-hsmm");
		//textToSpeech.setVoice("cmu-rms-hsmm");


		// Start recognition process pruning previously cached data.
		recognizer.startRecognition(true);

		// Start the Thread
		startSpeechThread();
		startResourcesThread();
		
		b1.addActionListener(this);
		
		jf.setVisible(true);
		jf.setSize(800,600);
		jf.setLocationRelativeTo(null);
		jf.setDefaultCloseOperation(jf.EXIT_ON_CLOSE);
		
	}

	/**
	 * Starting the main Thread of speech recognition
	 */
	protected void startSpeechThread() {

		// alive?
		if (speechThread != null && speechThread.isAlive())
			return;

		// initialise
		speechThread = new Thread(() -> {
			String str="Welcome To 5th Generation Computer";
			l1.setText(str);
			textToSpeech.speak(str, 1.5f, false, true);
			
			//System.out.println(str);
			
			logger.log(Level.INFO, "You can start to speak...\n");
			textToSpeech.speak("Voice command is active", 1.5f, false, true);
			try {
				while (true) {
					/*
					 * This method will return when the end of speech is
					 * reached. Note that the end pointer will determine the end
					 * of speech.
					 */
					SpeechResult speechResult = recognizer.getResult();
					if (speechResult != null) {

						result = speechResult.getHypothesis();
						System.out.println("You said: [" + result + "]\n");
						l2.setText(result);
						makeDecision(result);
						// logger.log(Level.INFO, "You said: " + result + "\n")

					} else
						logger.log(Level.INFO, "I can't understand what you said.\n");
				
				}
			} catch (Exception ex) {
				logger.log(Level.WARNING, null, ex);
			}

			logger.log(Level.INFO, "SpeechThread has exited...");
		});

		// Start
		speechThread.start();

	}

	/**
	 * Starting a Thread that checks if the resources needed to the
	 * SpeechRecognition library are available
	 */
	
	
	public void stopSpeechThread() {
		// alive?
		if (speechThread != null && speechThread.isAlive()) {
			recognizerStopped = true;
			//recognizer.stopRecognition(); it will throw error ;)
		}
	}

	
	
	protected void startResourcesThread() {

		// alive?
		if (resourcesThread != null && resourcesThread.isAlive())
			return;

		resourcesThread = new Thread(() -> {
			try {

				// Detect if the microphone is available
				while (true) {
					if (AudioSystem.isLineSupported(Port.Info.MICROPHONE)) {
						// logger.log(Level.INFO, "Microphone is available.\n")
					} else {
						// logger.log(Level.INFO, "Microphone is not
						// available.\n")

					}

					// Sleep some period
					Thread.sleep(350);
				}

			} catch (InterruptedException ex) {
				logger.log(Level.WARNING, null, ex);
				resourcesThread.interrupt();
			}
		});

		// Start
		resourcesThread.start();
	}

	/**
	 * Takes a decision based on the given result
	 * @throws IOException 
	 * @throws MalformedURLException 
	 * @throws CannotRealizeException 
	 * @throws NoPlayerException 
	 */
	public void makeDecision(String speech) throws NoPlayerException, CannotRealizeException, MalformedURLException, IOException {

		if ("who are you".equalsIgnoreCase(speech))
		{
			String s1="I am Jarvis,I can do anything to help you that!";
			
			l3.setText(s1);
			textToSpeech.speak(s1, 1.5f, false, true);
			
			return;
		}
		else if ("how are you".equalsIgnoreCase(speech))
		{
			String s3="I am Good, what about you?";
			l3.setText(s3);
			textToSpeech.speak(s3, 1.5f, false, true);
			return;
		}
		
		else if ("who is your boss".equalsIgnoreCase(speech))
		{
			String s4="Ved Prakash";
			l3.setText(s4);
			textToSpeech.speak(s4, 1.5f, false, true);
			return;
		}
		
		else if ("what are you doing".equalsIgnoreCase(speech))
		{
			String s5="I am Exploring!!!";
			l3.setText(s5);
			textToSpeech.speak(s5, 1.5f, false, true);
			return;
		}
		
		else if ("where are you living".equalsIgnoreCase(speech))
		{
			String s6="I am living in the heart of computer system";
			l3.setText(s6);
			textToSpeech.speak(s6, 1.5f, false, true);
			return;
		}
		
		else if ("hi".equalsIgnoreCase(speech))
		{
			String s7="hello, good evening!!";
			l3.setText(s7);
			textToSpeech.speak(s7, 1.5f, false, true);
			return;
		}
		
		else if ("hello jarvis".equalsIgnoreCase(speech))
		{
			String s8="hii Boss, what can i do for you ?";
			l3.setText(s8);
			textToSpeech.speak(s8, 1.5f, false, true);
			return;
		}
		
		else if ("where i am from".equalsIgnoreCase(speech))
		{
			String s9="You are from Bihar";
			l3.setText(s9);
			textToSpeech.speak(s9, 1.5f, false, true);
			return;
		}
		
		else if ("who developed you".equalsIgnoreCase(speech))
		{
			String s10="Ved Prakash";
			l3.setText(s10);
			textToSpeech.speak(s10, 1.5f, false, true);
			return;
		}
		
		else if ("what is your favourite colour".equalsIgnoreCase(speech))
		{
			String s11="My favourite colour is blue,green,yellow,red";
			l3.setText(s11);
			textToSpeech.speak(s11, 1.5f, false, true);
			return;
		}
		
		else if ("are you lie".equalsIgnoreCase(speech))
		{
			String s12="Nope never never!!!";
			l3.setText(s12);
			textToSpeech.speak(s12, 1.5f, false, true);
			return;
		}
		
		else if ("what is your language".equalsIgnoreCase(speech))
		{
			String s13="Java.";
			l3.setText(s13);
			textToSpeech.speak(s13, 1.5f, false, true);
			return;
		}
		
		else if ("who is your Teacher".equalsIgnoreCase(speech))
		{
			String s14="You!!";
			l3.setText(s14);
			textToSpeech.speak(s14, 1.5f, false, true);
			return;
		}
		
		else if ("open paint".equalsIgnoreCase(speech))
		{
			Process p;
			textToSpeech.speak("Opening Paint", 1.5f, false,true);
			p=Runtime.getRuntime().exec("mspaint.exe");
			
			//textToSpeech.speak(s14, 1.5f, false, true);
			return;
		}
		
		else if ("close paint".equalsIgnoreCase(speech))
		{
			Process p;
			textToSpeech.speak("Closing Paint", 1.5f, false,true);
			p=Runtime.getRuntime().exec("TASKKILL /F /IM mspaint.exe");
			
			//textToSpeech.speak(s14, 1.5f, false, true);
			return;
		}
		
		
		else if("task manager".equalsIgnoreCase(speech))
			{	 try{
		        Process p;
		    	//resultText="";
		        textToSpeech.speak("Opening Task Manager", 1.5f, false,true);
		        p = Runtime.getRuntime().exec("cmd /c start taskmgr.exe");
		       // System.out.println("inside");
		        }catch(Exception ae){}
			}
		
		else if("close task manager".equalsIgnoreCase(speech))
			{	 try{
		        Process p;
		    	//resultText="";
		        textToSpeech.speak("Closing Task Manager", 1.5f, false,true);
		        p = Runtime.getRuntime().exec("TASKKILL /F/IM taskmgr.exe");
		       // System.out.println("inside");
		        }catch(Exception ae){}
			}
		else if ("open pad".equalsIgnoreCase(speech))
		    {
		        try{
		        Process p;	//resultText="";
		        textToSpeech.speak("Opening NotePad", 1.5f, false,true);
		        p = Runtime.getRuntime().exec("cmd /c start notepad");
		       // System.out.println("inside");
		        }catch(Exception ae){}
		    }
		
		
		else if ("close pad".equalsIgnoreCase(speech))
		    {
		        try{
		        Process p;	//resultText="";
		        textToSpeech.speak("Closing NotePad", 1.5f, false,true);
		        p = Runtime.getRuntime().exec("cmd /c start taskkill /im notepad.exe /f");
		       // System.out.println("inside");
		        }catch(Exception ae){}
		    }
		
		else if ("browser".equalsIgnoreCase(speech))
		{
			Process p;
			textToSpeech.speak("Opening Browser", 1.5f, false,true);
			p=Runtime.getRuntime().exec("cmd /c start firefox.exe");
			
			//textToSpeech.speak(s14, 1.5f, false, true);
			return;
		}
		
		else if ("close browser".equalsIgnoreCase(speech))
		{
			Process p;
			textToSpeech.speak("Closing Browser", 1.5f, false,true);
			p=Runtime.getRuntime().exec("cmd /c start taskkill /im firefox.exe /f");
			
			//textToSpeech.speak(s14, 1.5f, false, true);
			return;
		}
		
		else if ("who is your mom".equalsIgnoreCase(speech))
		{
			String s15="C.P.U.";
			l3.setText(s15);
			textToSpeech.speak(s15, 1.5f, false, true);
			return;
		}
		
		else if ("play a song".equalsIgnoreCase(speech))
		{
			//jf.setContentPane(new JLabel(new ImageIcon("img\\mi.gif")));
			String s16="Sure";
			l3.setText(s16);
			textToSpeech.speak(s16, 1.5f, false, true);
			p1=Manager.createRealizedPlayer(new File("mp//Dj.wav").toURL());
			
			p1.start();
			
			return;
		}
		
		else if ("player".equalsIgnoreCase(speech))
		    {
		        try{
		        Process p;
		    	//resultText="";
		        p = Runtime.getRuntime().exec("cmd /c start wmplayer");
		        }catch(Exception ae){}
		    }
		
		else if("facebook".equalsIgnoreCase(speech))
			{	 try{
		        Process p;
		    	//resultText="";
		        textToSpeech.speak("Opening Facebook", 1.5f, false,true);
		        p = Runtime.getRuntime().exec("cmd /c start firefox www.facebook.com");
		       // System.out.println("inside");
			}catch(Exception ae){}
		}
		
		else if("car".equalsIgnoreCase(speech))
		{	 try
			{
	        Process p;
	    	//resultText="";
	        textToSpeech.speak("There are some search result", 1.5f, false,true);
	        p = Runtime.getRuntime().exec("cmd /c start firefox https://www.google.com/search?q=car&ie=utf-8&oe=utf-8&client=firefox-b");
	       // System.out.println("inside");
			}catch(Exception ae){}
		}
		
		else if ("next song".equalsIgnoreCase(speech))
		{
			//jf.setContentPane(new JLabel(new ImageIcon("img\\mi.gif")));
			String s16="Sure";
			l3.setText(s16);
			textToSpeech.speak(s16, 1.5f, false, true);
			p1=Manager.createRealizedPlayer(new File("mp//FG.wav").toURL());
			
			p1.start();
			
			return;
		}
		
		else if ("start excel".equalsIgnoreCase(speech))
		    {
		        try{
		        Process p;	//resultText="";
		        textToSpeech.speak("Opening Ms Excel", 1.5f, false,true);
		        p = Runtime.getRuntime().exec("cmd /c start excel");
		       // System.out.println("inside");
		        }catch(Exception ae){}
		     }
		else if ("excel".equalsIgnoreCase(speech))
	    {
	        try{
	        Process p;	//resultText="";
	        textToSpeech.speak("Opening Excel", 1.5f, false,true);
	        p = Runtime.getRuntime().exec("cmd /c start excel");
	       // System.out.println("inside");
	        }catch(Exception ae){}
	     }
		
		else if ("stop excel".equalsIgnoreCase(speech))
			    {
			        try{
			        Process p;	//resultText="";
			        textToSpeech.speak("Closing Excel", 1.5f, false,true);
			        p = Runtime.getRuntime().exec("cmd /c start taskkill /im excel.exe /f");
			       // System.out.println("inside");
			        }catch(Exception ae){}
			    }
		else if ("close excel".equalsIgnoreCase(speech))
	    {
	        try{
	        Process p;	//resultText="";
	        textToSpeech.speak("Closing Excel", 1.5f, false,true);
	        p = Runtime.getRuntime().exec("cmd /c start taskkill /im excel.exe /f");
	       // System.out.println("inside");
	        }catch(Exception ae){}
	    }
		
		
		else if("site mail".equalsIgnoreCase(speech))
			{	 try{
		        Process p;
		    	//resultText="";
		        textToSpeech.speak("Opening Gmail", 1.5f, false,true);
		        p = Runtime.getRuntime().exec("cmd /c start firefox https://mail.google.com");
		       // System.out.println("inside");
		        }catch(Exception ae){}
			}
		
		else if("mail".equalsIgnoreCase(speech))
		{	 try{
	        Process p;
	    	//resultText="";
	        p = Runtime.getRuntime().exec("cmd /c start firefox https://mail.google.com");
	       // System.out.println("inside");
	        }catch(Exception ae){}
		}
		
		else if("sing a song".equalsIgnoreCase(speech))
		{	 try{
	        Process p;
	    	//resultText="";
	        textToSpeech.speak("Sure", 1.5f, false,true);
	       // p = Runtime.getRuntime().exec("cmd /c start firefox https://mail.google.com");
	       // System.out.println("inside");
	        p1=Manager.createRealizedPlayer(new File("mp//Twinkle.wav").toURL());
	    	p1.start();
	        textToSpeech.speak("Twinkle,,,,,!!!...... Twinkle,,,,,.....!!! Little star,... How,, are,,,...! wonder,,,,!!!! what,,,,.. you,,,.. are,....!! up,.,., above,,.,.,.,.!!! the,.,.,!!! world,,,...!!! so,,,... high,.. like,.,.!!!.,. a,., diamond,.,.,.!!!,,, in,,,... the,,,,.... sky,,.,.!!!!",1.5f,false,true);
	       
			//p1.stop();
	        }catch(Exception ae){}
		}
		
		
		String[] array = speech.split("(plus|minus|multiply|division){1}");
		System.out.println(Arrays.toString(array) + array.length);
		// return if user said only one number
		if (array.length < 2)
			return;
		
		
		

		
		
		// Find the two numbers
		System.out.println("Number one is:" + stringToNumber.convert(array[0]) + " Number two is: "
				+ stringToNumber.convert(array[1]));
		int number1 = stringToNumber.convert(array[0]);// .convert(array[0])
		int number2 = stringToNumber.convert(array[1]);// .convert(array[1])

		// Calculation result in int representation
		int calculationResult = 0;
		String symbol = "?";

		// Find the mathematical symbol
		if (speech.contains("plus")) {
			calculationResult = number1 + number2;
			symbol = "+";
		} else if (speech.contains("minus")) {
			calculationResult = number1 - number2;
			symbol = "-";
		} else if (speech.contains("multiply")) {
			calculationResult = number1 * number2;
			symbol = "*";
		} else if (speech.contains("division")) {
			if (number2 == 0)
				return;
			calculationResult = number1 / number2;
			symbol = "/";
		}
		
		else
		{
			textToSpeech.speak("I can't understand what you said.", 1.5f, false, true);
		}
		
	

		String res = numberToString.convert(Math.abs(calculationResult));

		// With words
		System.out.println("Said:[ " + speech + " ]\n\t\t which after calculation is:[ "
				+ (calculationResult >= 0 ? "" : "minus ") + res + " ] \n");

		// With numbers and math
		System.out.println("Mathematical expression:[ " + number1 + " " + symbol + " " + number2
				+ "]\n\t\t which after calculation is:[ " + calculationResult + " ]");


		l3.setText(res);
		// Speak Mary Speak
		textToSpeech.speak((calculationResult >= 0 ? "" : "minus ") + res, 1.5f, false, true);
		
		
		
		
		
		

	}

	/**
	 * Java Main Application Method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		// // Be sure that the user can't start this application by not giving
		// the
		// // correct entry string
		// if (args.length == 1 && "SPEECH".equalsIgnoreCase(args[0]))
		new Main();
		// else
		// Logger.getLogger(Main.class.getName()).log(Level.WARNING, "Give me
		// the correct entry string..");

	}
	@Override
	public void actionPerformed(ActionEvent ae) 
	{
		if(ae.getSource()==b1)
		{
			p1.stop();
		}
		
	}

}