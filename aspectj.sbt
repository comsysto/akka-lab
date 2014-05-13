import com.typesafe.sbt.SbtAspectj._

aspectjSettings

// According to kamon documentation - does not work with sbt run
// javaOptions <++= AspectjKeys.weaverOptions in Aspectj

// According to https://github.com/sbt/sbt-aspectj/tree/master/src/sbt-test/weave/load-time
// This works with sbt run
fork in run := true

javaOptions in run <++= AspectjKeys.weaverOptions in Aspectj