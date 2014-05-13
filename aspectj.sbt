import com.typesafe.sbt.SbtAspectj._

aspectjSettings

javaOptions <++= AspectjKeys.weaverOptions in Aspectj