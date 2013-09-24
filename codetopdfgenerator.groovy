//depends on enscript and ps2pdf installed

@Grab(group='com.lowagie', module='itext', version='4.2.0')
import com.lowagie.text.Document
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfWriter
import com.lowagie.text.Chunk
import com.lowagie.text.Font
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfCopy
import java.awt.Color
import groovy.io.FileType

tmpFolder = new File(System.getProperty("java.io.tmpdir"),"tmp${UUID.randomUUID().toString()}")
def tmpFolderCreated = tmpFolder.mkdirs()

try{

    if (!tmpFolderCreated) {
        throw new RuntimeException("Unable to create temporary folder.")
    }

    def commandLineArguments = parseCommandLineArguments(args)

    def sourceCodeInput = findSourceCodeFiles(commandLineArguments.inputFolder,commandLineArguments.ignoreFolders)

    def pdfFiles = createPdfFiles(sourceCodeInput, commandLineArguments.outputFile.getParentFile())

    def titlePagePDF = createTitlePage()

    appendSourceCodePDF(titlePagePDF, pdfFiles, commandLineArguments.outputFile)
} finally {
    cleanup()
}


def appendSourceCodePDF(titlePage, srcPdfFiles, outputFile) {
    if (!titlePage.exists()) {
        throw new RuntimeException("title page wasn't created successfully. FATAL")
    }
    Document document = new Document()
    PdfCopy copy = new PdfCopy(document, new FileOutputStream(outputFile)) 
    document.open()
    
    PdfReader reader = new PdfReader(titlePage.getAbsolutePath())
    copy.addPage(copy.getImportedPage(reader, 1))

    (0..(srcPdfFiles.size()-1)).each { fileIndex ->
        reader = new PdfReader(srcPdfFiles[fileIndex].getAbsolutePath())
        int n = reader.getNumberOfPages()
        for (int page = 0 ; page < n; ) {
            copy.addPage(copy.getImportedPage(reader, ++page))
        }
        copy.freeReader(reader)
        reader.close()
    }
    document.close()
}

def createTitlePage() {
    def titlePageInfo = promptForTitlePageInfo()
    def document = new Document()

    Font hyperlinkFont = new Font();
    hyperlinkFont.setColor(Color.BLUE);
    hyperlinkFont.setStyle(Font.UNDERLINE);  
    
    File outputFile = new File(tmpFolder, "title_tmp.pdf")

    PdfWriter.getInstance(document,new FileOutputStream(outputFile))

    document.open() 
    document.add(new Paragraph("Assignment #${titlePageInfo.assignmentNumber as String}"))
    document.add(new Paragraph(titlePageInfo.userId))
    document.add(new Paragraph(titlePageInfo.fullname))
    document.add(new Paragraph(new Chunk(titlePageInfo.email,hyperlinkFont).setAnchor(titlePageInfo.email)))
    document.add(new Paragraph(titlePageInfo.courseNumber))
    document.add(new Paragraph(new Chunk(titlePageInfo.url,hyperlinkFont).setAnchor(titlePageInfo.url)))
    document.close()

    outputFile
}

def createPdfFiles(inputFiles, outputFolder) {
    def tmpFilename = "a"
    inputFiles.each{ file -> 
        def tempPostscriptFile = new File(tmpFolder,tmpFilename+'.ps')
        "enscript --color ${file.getAbsolutePath()} -E${getProgrammingLanguage(file)} -o ${tempPostscriptFile.getAbsolutePath()}".execute().waitFor()
        if (System.properties['os.name'].toLowerCase().indexOf("mac") >= 0) {
            "pstopdf ${tempPostscriptFile} -o ${new File(tmpFolder,tmpFilename+'.pdf')}".execute().waitFor()
        } else {
            "ps2pdf ${tempPostscriptFile} ${new File(tmpFolder,tmpFilename+'.pdf')}".execute().waitFor()
        }
        if (!new File(tmpFolder,tmpFilename+'.pdf').exists()) {
            throw new RuntimeException("Could not convert ps to pdf");
        }
        if (!tempPostscriptFile.delete()) {
            throw new RuntimeException("Could not delete temp file ${tempPostscriptFile.getAbsolutePath()}")
        }
        tmpFilename++
    }
    def tmpFilesInOrder = tmpFolder.listFiles() as List
    def filenameComparator = { a,b -> a.name.compareTo(b.name) }
    tmpFilesInOrder.sort(filenameComparator) 
    
}

def getProgrammingLanguage(file) {
    if (file.name.endsWith("html") || file.name.endsWith("xhtml")) {
        return "html"
    } else if (file.name.endsWith("java")) {
        return "java"
    } else if (file.name.endsWith("js") || file.name.endsWith("javascript")) {
        return "java"
    } else if (file.name.endsWith("css")) {
        return "javascript"
    } else if (file.name.endsWith("jsp")) {
        return "html"
    } else if (file.name.endsWith("xml")) {
        return "xml"
    }
}

//This will return source code files in order that is required
//for ajax class. html first, jsp second, then javascript, java next and css last
def findSourceCodeFiles(inputFolder,ignoreFolders) {
    def fileList = []
    inputFolder.eachFileRecurse(FileType.FILES) { file ->
        def fileSplit = file.name.split("\\.")
        if (fileSplit.length > 1) {
            if (["java","css","html","xhtml","xml","java","jsp","css","js","javascript"].contains(fileSplit[1].toLowerCase())) {
                fileList << file
            }
        }
    } 
    def comparator = { a,b ->
        def aName = a.name
        def bName = b.name
        
        def aExtension = a.name.split("\\.")[1].toLowerCase()
        def bExtension = b.name.split("\\.")[1].toLowerCase()
        if (aExtension == bExtension) {
            return 0
        }   
        if (aExtension == "html") {
            return -1
        } 
        if (bExtension == "html") {
            return 1
        }
        if (aExtension == "xhtml") {
            return -1
        } 
        if (bExtension == "xhtml") {
            return 1
        }
        if (aExtension == "xml") {
            return -1
        } 
        if (bExtension == "xml") {
            return 1
        }
        if (aExtension == "jsp") {
            return -1
        }
        if (bExtension == "jsp") {
            return 1
        }
        if (aExtension == "js") {
            return -1
        }
        if (bExtension == "js") {
            return 1
        }
        if (aExtension == "javascript") {
            return -1
        }
        if (bExtension == "javascript") {
            return 1
        }
        if (aExtension == "java") {
            return -1
        }
        if (bExtension == "java") {
            return 1
        }
        if (aExtension == "css") {
            return -1
        }
        if (bExtension == "css") {
            return 1
        }
    } as Comparator
    fileList.sort(comparator)
}

def parseCommandLineArguments(args) {
    def cli = new CliBuilder(usage:'groovy codetopdfgenerator.groovy [options]')
    cli.with {
        o longOpt: 'output-file','Output filename (must end in .pdf)',required:true,args:1
        i longOpt: 'input-folder','Folder with your source code',required:true,args:1
        n longOpt: 'ignore-folders','Comma separated list of folders to omit (for example target folder or bin folder).',args:1
    }
    def options = cli.parse(args)
    if (!options) {
        System.exit(-1)
    }
    def ignoreFolders = []
    if (options.n) {
        ignoreFolders = options.n
        ignoreFolders = ignoreFolders.split(",")
    }
    def inputFolder = new File(options.i)    
    if (!inputFolder.exists()) {
        println "Input folder does not exist!"
        cli.usage()
        System.exit(-1)
    }
    def outputFile = new File(options.o)
    if (!outputFile.getName().endsWith(".pdf")){
        println "Output file must end in .pdf"
        cli.usage()
        System.exit(-1)
    }
    [ignoreFolders:ignoreFolders,inputFolder:inputFolder,outputFile:outputFile]
}

def promptForTitlePageInfo() {
    def reader = new BufferedReader(new InputStreamReader(System.in))
    def titleInformation = [:]
    print "Enter assignment number: "
    titleInformation.assignmentNumber = readInteger(reader) 
    print "Enter server userid: "
    titleInformation.userId = readString(reader)
    print "Enter fullname: "
    titleInformation.fullname =readString(reader)
    print "Enter email address: "
    titleInformation.email = readString(reader)
    print "Enter course number (605.787): "
    titleInformation.courseNumber = readString(reader) 
    print "Enter URL to project: "
    titleInformation.url = readString(reader)

    return titleInformation
}   

def readString(reader) {
    while (true) {
        def string = reader.readLine()
        if (string) {
            return string 
        }
        print "Please enter a string: "
    }
}

def readInteger(reader) {
    while (true) {
        try {
            return reader.readLine() as Integer 
        } catch (Exception e) {
            print "Please enter a valid number: "
        }
    }
}

def cleanup() {
    try {
        def delClos

        delClos = { 
                    it.eachDir( delClos );
                    it.eachFile {
                        it.delete()
                    }
                it.delete()
        }

        delClos(tmpFolder)
    } catch (Exception e){}
}
