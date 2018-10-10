import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

import java.sql.Date


/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "com.qianmi.uc.qstore.domain.store"
beanName ="SceneBean"

typeMapping = [
        (~/(?i)int/)                      : "Integer",
        (~/(?i)long/)                      : "String",
        (~/(?i)number/)                      : "Long",
        (~/(?i)float|double|decimal|real/): "Double",
        (~/(?i)datetime|timestamp/)       : "LocalDateTime",
        (~/(?i)date/)                     : "LocalDate",
        (~/(?i)time/)                     : "String",
        (~/(?i)/)                         : "String"
]


FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
}



def generate(table, dir) {
  def className = javaName(beanName, true)
  def fields = calcFields(table)
  new File(dir, className + ".java").withPrintWriter { out -> generate(out, className, fields,table) }
}

def generate(out, className, fields,table) {
  out.println "package $packageName ;"
  out.println ""
  out.println "import lombok.AllArgsConstructor;\n" +
              "import lombok.Builder;\n" +
              "import lombok.EqualsAndHashCode;\n" +
              "import lombok.Getter;\n" +
              "import lombok.NoArgsConstructor;\n" +
              "import lombok.Setter;\n" +
              "import lombok.ToString;\n" +
              "\n" +
              "import java.io.Serializable;\n" +
              "import java.time.LocalDate;\n" +
              "import java.time.LocalDateTime;\n" +
              "import javax.persistence.Column;\n" +
              "import javax.persistence.Entity;\n" +
              "import javax.persistence.Table;"
  out.println ""
  out.println "/**\n" +
              " * Entidade referente à tabela do banco de dados.\n" +
              " */"
  out.println "@Getter"
  out.println "@Setter"
  out.println "@Builder"
  out.println "@AllArgsConstructor"
  out.println "@NoArgsConstructor"
  out.println "@EqualsAndHashCode(exclude = {})"
  out.println "@ToString(exclude = {})"
  out.println "@Table (name = \""+ table.getName() +"\")"
  out.println "@Entity"
  out.println "public class $className  implements Serializable {"
  out.println ""
  out.println "    " + genSerialID()
  out.println ""
  fields.each() {

    if (it.annos.size() >0)
    {
      it.annos.each() {
        out.println "    ${it}"
      }
    }
    out.println "    private ${it.type} ${it.name};"
    out.println ""
  }
  out.println ""

  out.println "}"
}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())

    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    def comm =[
            name :  changeStyle (javaName(col.getName(), false) ,true),
            type : typeStr,
            annos: ["@Column(name = \""+col.getName()+"\" )"]]
    if("id".equals(Case.LOWER.apply(col.getName())))
      comm.annos +=["@Id"]
    fields += [comm]



  }
}

def javaName(str, capitalize) {
  def s = str.split(/(?<=[^\p{IsLetter}])/).collect { Case.LOWER.apply(it).capitalize() }
          .join("").replaceAll(/[^\p{javaJavaIdentifierPart}]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
static String changeStyle(String str, boolean toCamel){
  if(!str || str.size() <= 1)
    return str

  if(toCamel){
    String r = str.toLowerCase().split('_').collect{cc -> Case.LOWER.apply(cc).capitalize()}.join('')
    return r[0].toLowerCase() + r[1..-1]
  }else{
    str = str[0].toLowerCase() + str[1..-1]
    return str.collect{cc -> ((char)cc).isUpperCase() ? '_' + cc.toLowerCase() : cc}.join('')
  }
}
static String genSerialID()
{
  return "private static final long serialVersionUID =  "+Math.abs(new Random().nextLong())+"L;";
}