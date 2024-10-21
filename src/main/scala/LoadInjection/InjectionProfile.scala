package LoadInjection

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map

object InjectionProfile extends Simulation{

  def LoadTestInjectionProfile(scnMap: Map[io.gatling.core.structure.ScenarioBuilder, Int] , httpProtocolMap: Map[io.gatling.core.structure.ScenarioBuilder, io.gatling.http.protocol.HttpProtocolBuilder], ScenariosToRun:ListBuffer[io.gatling.core.structure.PopulationBuilder] ) : ListBuffer[io.gatling.core.structure.PopulationBuilder] = {
    for ((scn,user) <- scnMap) {
        val httpconf = httpProtocolMap.apply(scn)
            ScenariosToRun+=scn.inject(atOnceUsers(user)).protocols(httpconf)
    }
    return ScenariosToRun
  }

  def SclabilityTestInjectionProfile(ScaleInputs: String , httpProtocolMap: Map[io.gatling.core.structure.ScenarioBuilder, io.gatling.http.protocol.HttpProtocolBuilder], ScenariosToRun:ListBuffer[io.gatling.core.structure.PopulationBuilder] ) : ListBuffer[io.gatling.core.structure.PopulationBuilder] = {
    val InputsForTest = ScaleInputs.split(",")

    val StartWithUsersCount = InputsForTest(0).toInt
    val StepUsersPerSec = InputsForTest(1).toInt
    val StepRampDuration = InputsForTest(2).toInt
    val StepDuration = InputsForTest(3).toInt
    val NoOfSteps = InputsForTest(4).toInt

    for ((scn,httpconf) <- httpProtocolMap) {
      ScenariosToRun+=scn.inject(
                      incrementConcurrentUsers(StepUsersPerSec)
                      .times(NoOfSteps)
                      .eachLevelLasting(StepDuration)
                      .separatedByRampsLasting(StepRampDuration)
                      .startingFrom(StartWithUsersCount)
                      ).protocols(httpconf)

    }
    return ScenariosToRun
  }  

  def TPSTestInjectionProfile(TPSInputs: String , httpProtocolMap: Map[io.gatling.core.structure.ScenarioBuilder, io.gatling.http.protocol.HttpProtocolBuilder], ScenariosToRun:ListBuffer[io.gatling.core.structure.PopulationBuilder] ) : ListBuffer[io.gatling.core.structure.PopulationBuilder] = {
    val InputsForTest = TPSInputs.split(",")

    val Users = InputsForTest(0).toInt
    val TargetTPS = InputsForTest(1).toInt
    val InitialRamp = InputsForTest(2).toInt
    val testDuration = InputsForTest(3).toInt

    for ((scn,httpconf) <- httpProtocolMap) {
      ScenariosToRun+=scn.inject(atOnceUsers(Users))
                      .throttle(reachRps(TargetTPS).in(InitialRamp), holdFor(testDuration.seconds))
                      .protocols(httpconf)

    }
    return ScenariosToRun
  }    


} 
  

