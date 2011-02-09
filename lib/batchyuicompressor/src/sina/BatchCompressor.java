package sina;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.ErrorReporter;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * 代码修改自YUI Compressor（BSD开源协议）;其中Mozilla Rhino 使用MPL开源协议
 * 批量压缩js和（或）css ,支持嵌套文件夹.
 * 
 * 使用: java -jar batchyuicompressor.jar
 * 
 * 注意:
 * 1)4个配置参数均在compress.ini文件中;
 * 2)yuicompressor的一些调用参数则写死了(默认UTF-8编码，不混淆等;依据实际使用及个人偏好也可以提取到配置文件中).
 * 
 * @author newdongyuwei@gmail.com|yuwei@staff.sina.com.cn
 * */

public class BatchCompressor {
	public static void main(String[] args) throws EvaluatorException,
			IOException {

		/**
		 * 
		 * 参数解析使用配置文件(4个配置参数，不混淆js) 
		 * 1) jsInputDir 
		 * 2) jsOutputDir 
		 * 3) cssInputDir
		 * 4) cssOutputDir
		 */
		String ini = System.getProperty("user.dir") + File.separator + "compress.ini";
		File conf = new File(ini);
		if (conf.exists()) {
			System.out.println("the config  file is : " + ini);
			Properties prop = new Properties();
			prop.load(new FileInputStream(conf));
			String jsInputDir = (String) prop.get("jsInputDir");
			String jsOutputDir = (String) prop.get("jsOutputDir");
			String cssInputDir = (String) prop.get("cssInputDir");
			String cssOutputDir = (String) prop.get("cssOutputDir");
			String obfuscate = (String) prop.get("obfuscate");
			boolean munge = false;
			if(obfuscate != null){
				if(obfuscate.trim().toLowerCase().equals("true")){
					munge = true;
				}
			}
			System.out.println(
					"\n-----------------------------------------------------------------------------\n"
					+ "jsInputDir: " + jsInputDir + "\n" 
					+ "jsOutputDir: " + jsOutputDir + "\n" 
					+ "cssInputDir: " + cssInputDir + "\n"
					+ "cssOutputDir: " + cssOutputDir+ "\n"
					+ "obfuscate：" + munge + "\n" +
					"\n-----------------------------------------------------------------------------\n");
			
			if (jsInputDir != null && jsOutputDir != null) {
				String[] jsInputDirs = jsInputDir.split(";");
				String[] jsOutputDirs = jsOutputDir.split(";");
				int len = jsInputDirs.length;
				if(len == jsOutputDirs.length){
					for(int i=0;i<len;i++){
						System.out.println("\n***************** start compress  js **************************\n");
						compressAllJS(jsInputDirs[i].trim(), jsOutputDirs[i].trim(), munge);
						System.out.println("\n****************** end compress  js ***************************\n");
					}
				}else{
					System.out.println("jsInputDir 和 jsOutputDir文件夹数量不匹配！请保持输入输出1:1对应比例");
				}
				
			}
			
			if (cssInputDir != null && cssOutputDir != null) {
				String[] cssInputDirs = cssInputDir.split(";");
				String[] cssOutputDirs = cssOutputDir.split(";");
				int len = cssInputDirs.length;
				if(len == cssOutputDirs.length){
					for(int i=0;i<len;i++){
						System.out.println("\n***************** start compress  css **************************\n");
						compressAllCSS(cssInputDirs[i].trim(), cssOutputDirs[i].trim());
						System.out.println("\n****************** end compress  css ***************************\n");
					}
				}else{
					System.out.println("cssInputDir 和 cssOutputDir文件夹数量不匹配！请保持输入输出1:1对应比例");
				}
			}
			
		} else {
			System.out.println("No compress.ini  exist, please check it!");
		}
	}

	/**
	 * 压缩所有js
	 * 
	 * @param inputDirPath
	 * @param outputDirPath
	 * @param munge:是否混淆(默认应该不混淆)
	 */
	public static void compressAllJS(String inputDirPath, String outputDirPath,
			boolean munge) throws EvaluatorException, IOException {
		File folder = new File(inputDirPath);
		recurseCompress(folder, inputDirPath, outputDirPath, munge, "js");

	}

	/**
	 * 压缩所有css
	 * 
	 * @param inputDirPath
	 * @param outputDirPath
	 */
	public static void compressAllCSS(String inputDirPath, String outputDirPath)
			throws EvaluatorException, IOException {
		File folder = new File(inputDirPath);
		recurseCompress(folder, inputDirPath, outputDirPath, false, "css");
	}

	/**
	 * 递归压缩js或者css
	 * 
	 * @param folder
	 * @param inputDirPath
	 * @param outputDirPath
	 * @param munge:是否混淆
	 * @param type:"js" or "css"
	 */
	private static void recurseCompress(File folder, String inputDirPath,String outputDirPath, boolean munge, String type)
			throws IOException {
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				String inputFile = listOfFiles[i].getAbsolutePath();
				String outputFile = inputFile.replace(inputDirPath,
						outputDirPath);
				File file = new File(outputFile);
				if (file.exists() && file.getName().indexOf(type) != -1) {
					file.delete();// 如果存在同名旧文件，先删除旧的文件
				} else if (file.getName().indexOf(type) != -1) {
					//windows下面可能需要先创建上级目录(ubuntu下面其实不需要这一步)
					(new File(file.getAbsolutePath().replace(file.getName(), ""))).mkdirs();
					file.createNewFile();
				}

				if (file.getName().indexOf(type) != -1 && type.equalsIgnoreCase("js")) {
					compressOneJS(inputFile, outputFile, munge);
				} else if (file.getName().indexOf(type) != -1 && type.equalsIgnoreCase("css")) {
					compressOneCSS(inputFile, outputFile);
				}
			} else if (listOfFiles[i].isDirectory()) {
				File file = new File(listOfFiles[i].getAbsolutePath().replace(inputDirPath, outputDirPath));
				if (!file.exists()) {
					file.mkdirs();// 递归创建目录，类似 "mkdir -p" shell 命令
				}
				recurseCompress(listOfFiles[i], inputDirPath, outputDirPath,munge, type);
			}
		}
	}

	/**
	 * 压缩一个js
	 * 
	 * @param inputFileName
	 * @param outputFileName
	 * @param munge
	 */
	private static void compressOneJS(final String inputFileName,String outputFileName, boolean munge) throws EvaluatorException,
			IOException {
		Reader in = new InputStreamReader(new FileInputStream(inputFileName),"UTF-8");
		JavaScriptCompressor compressor = new JavaScriptCompressor(in,
				new ErrorReporter() {
					public void warning(String message, String sourceName,
							int line, String lineSource, int lineOffset) {
						if (line < 0) {
							System.err.println("\n[WARNING] " + message);
						} else {
							System.err.println("\n[WARNING] " + line + ':'
									+ lineOffset + ':' + message);
						}
					}

					public void error(String message, String sourceName,
							int line, String lineSource, int lineOffset) {
						if (line < 0) {
							throw new Error("\n[ERROR] " + message + " 出错文件是: " + inputFileName);
						} else {
							throw new Error("\n[ERROR] 文件" + inputFileName + "第" + line + "行出错:" +  message);
						}
					}

					public EvaluatorException runtimeError(String message,
							String sourceName, int line, String lineSource,
							int lineOffset) {
						error(message, sourceName, line, lineSource, lineOffset);
						return new EvaluatorException(message);
					}
				});

		in.close();
		in = null;

		Writer out = new OutputStreamWriter(new FileOutputStream(outputFileName), "UTF-8");

		// boolean munge = true;//混淆
		boolean preserveAllSemiColons = false;
		boolean disableOptimizations = false;
		boolean verbose = false;// 关闭详细信息（因为可能有大量警告信息）
		int linebreakpos = -1;// 无断行
		System.out.println("compress " + inputFileName);
		compressor.compress(out, linebreakpos, munge, verbose,preserveAllSemiColons, disableOptimizations);
		out.close();
	}

	/**
	 * 压缩一个css
	 * 
	 * @param inputFileName
	 * @param outputFileName
	 */
	private static void compressOneCSS(String inputFileName,String outputFileName) throws IOException {
		Reader in = new InputStreamReader(new FileInputStream(inputFileName),"UTF-8");
		CssCompressor compressor = new CssCompressor(in);
		in.close();
		in = null;

		Writer out = new OutputStreamWriter(new FileOutputStream(outputFileName), "UTF-8");
		int linebreakpos = -1;
		System.out.println("compress " + inputFileName);
		compressor.compress(out, linebreakpos);
		out.close();
	}
}
