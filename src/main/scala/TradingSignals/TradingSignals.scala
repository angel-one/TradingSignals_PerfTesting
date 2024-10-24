
package TradingSignals

import io.gatling.core.Predef._
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import java.time.Instant

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random
import scala.collection.mutable.ListBuffer

import LoadInjection.InjectionProfile

class ConditionalOrdersAPI extends Simulation {

  val user: Int   = System.getProperty("USER", "1").toInt

  val scenariosToExecute: String = System.getProperty("API_Name", "CRUD_APIs").toString // Picking input from Jenkins job (APIs to be executed)
  val Test_Model: String = System.getProperty("TestModel", "Load_Test").toString // Picking input from Jenkins job (Test Type: Load/Scalability/Spike)
  val Test_Inputs: String = System.getProperty("TestInputs", "10,1,1").toString // Picking input from Jenkins job (Test Type: StartWithUsersCount,StepUsersPerSec,StepRampDuration,StepDuration,NoOfSteps)
  val InputsForTest = Test_Inputs.split(",") 

  val httpconf_coreUrl = http.baseUrl("https://ts-sv-uat.angelone.in").userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36")
  val NTT_Tokens = csv("data/NTT_Tokens.csv").circular
  val NSE_CM_TOKENS = csv("data/NSE_CM_TOKENS_MINI.csv").circular

def Token_API()={
  feed(NTT_Tokens)
    .exec(http("Token")
      .post("/internal/token")
      .header("X-source", "spark")
      .header("Content-Type", "application/json")
      .body(StringBody(
        """{
          | "country_code": "IN",
          | "mob_no": "7352158126",
          | "user_id": "${pClientID}",
          | "source": "spark",
          | "app_id": "spark"
          | }""".stripMargin))
      .check(status is 200)
      .check(jsonPath("$.token").saveAs("Token"))
      .check(bodyString.saveAs("ResponseBody")) // Save the entire response body
    )
    .exec(session => {
      val responseBody = session("ResponseBody").as[String]
      println(s"Response body: $responseBody")
      session
    })
}


def GetSchema_API() = {
  exec(http("Get Schema")
    .get("/v1/schema")
    .header("accept", "application/json")
    .header("AccessToken", "#{Token}")
    .header("X-source", "spark")
    .queryParam("type", "ticked")
    .check(status is 200)
    .check(substring("success").exists))
}

def ConditionFeed_API() = {
  feed(NSE_CM_TOKENS)
        .exec(http("Condition Feed")
        .post("/v1/condition/results/feed?limit=3&sortOrder=desc&offset=0")
        .header("accept", "application/json")
        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhbmdlbCIsImV4cCI6MTc1OTk4OTY5MywiaWF0IjoxNzI4NDUyNjYwLCJ1c2VyRGF0YSI6eyJjb3VudHJ5X2NvZGUiOiIiLCJtb2Jfbm8iOiI5OTA1MDQ5MDY3IiwidXNlcl9pZCI6IlM2MTA0MTQ4OCIsInNvdXJjZSI6IjMiLCJhcHBfaWQiOiJzcGFyay13ZWIiLCJjcmVhdGVkX2F0IjoiMjAyNC0xMC0wOVQxMToxNDoyMC45MDQ0MDQwNCswNTozMCIsImRhdGFDZW50ZXIiOiIifSwidXNlcl90eXBlIjoiY2xpZW50IiwidG9rZW5fdHlwZSI6Im5vbl90cmFkZV9hY2Nlc3NfdG9rZW4iLCJzb3VyY2UiOiIzIiwiZGV2aWNlX2lkIjoiODAxNTBkMWEtYzFlZC01ZjUzLTkxNGMtYmIzYzk4MzM2OWM5IiwiYWN0Ijp7fSwicHJvZHVjdHMiOnsiZGVtYXQiOnsic3RhdHVzIjoiYWN0aXZlIn0sIm1mIjp7InN0YXR1cyI6ImFjdGl2ZSJ9fX0.V5EaNFeh7yp2bgZwwhBr1zZGNBxkfW1cnTFGwl9h-L0")
        .header("X-source", "spark")
        .header("Content-Type", "application/json")
        .body(StringBody(
          """{
            |    "universe": {
            |        "type": "",
            |        "underlying": {}
            |    },
            |    "source": "SIGNAL-FEED",
            |    "from": 1729621800000,
            |    "to": 1729708199000
            |}""".stripMargin))
        .check(status is 200)
      )
}

  def ConditionsFeed_Bulk_API() = {
    feed(NSE_CM_TOKENS)
      .exec(http("Condition Bulk Feed")
        .post("/v1/condition/results/feed?limit=1000&sortOrder=desc&offset=0")
        .header("accept", "application/json")
        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJhbmdlbCIsImV4cCI6MTc1OTk4OTY5MywiaWF0IjoxNzI4NDUyNjYwLCJ1c2VyRGF0YSI6eyJjb3VudHJ5X2NvZGUiOiIiLCJtb2Jfbm8iOiI5OTA1MDQ5MDY3IiwidXNlcl9pZCI6IlM2MTA0MTQ4OCIsInNvdXJjZSI6IjMiLCJhcHBfaWQiOiJzcGFyay13ZWIiLCJjcmVhdGVkX2F0IjoiMjAyNC0xMC0wOVQxMToxNDoyMC45MDQ0MDQwNCswNTozMCIsImRhdGFDZW50ZXIiOiIifSwidXNlcl90eXBlIjoiY2xpZW50IiwidG9rZW5fdHlwZSI6Im5vbl90cmFkZV9hY2Nlc3NfdG9rZW4iLCJzb3VyY2UiOiIzIiwiZGV2aWNlX2lkIjoiODAxNTBkMWEtYzFlZC01ZjUzLTkxNGMtYmIzYzk4MzM2OWM5IiwiYWN0Ijp7fSwicHJvZHVjdHMiOnsiZGVtYXQiOnsic3RhdHVzIjoiYWN0aXZlIn0sIm1mIjp7InN0YXR1cyI6ImFjdGl2ZSJ9fX0.V5EaNFeh7yp2bgZwwhBr1zZGNBxkfW1cnTFGwl9h-L0")
        .header("X-source", "spark")
        .header("Content-Type", "application/json")
        .body(StringBody(
          """{
            |    "universe": {
            |        "type": "",
            |        "underlying": {}
            |    },
            |    "source": "SIGNAL-FEED",
            |    "from": 1729621800000,
            |    "to": 1729708199000
            |}""".stripMargin))
        .check(status is 200)
      )
  }

  def CallBack_Api() = {
    feed(NSE_CM_TOKENS)
      .exec(http("Call Back Api")
        .post("/v1/condition/callback")
        .header("accept", "application/json")
        .header("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX3R5cGUiOiJhcHBsaWNhdGlvbiIsInRva2VuX3R5cGUiOiJzMnNfYWNjZXNzX3Rva2VuIiwiZ21faWQiOjAsImtpZCI6ImNvbmRpdGlvbmFsLW9yZGVycy1zZXJ2aWNlLWtleS12MSIsIm9tbmVtYW5hZ2VyaWQiOjAsImlzcyI6ImNvbmRpdGlvbmFsLW9yZGVycy1zZXJ2aWNlIiwic3ViIjoiY29uZGl0aW9uYWwtb3JkZXJzIiwiYXVkIjpbInRyYWRpbmctc2lnbmFscyIsInRzLXNjcmlwdmVyc2UiXSwiZXhwIjoxNzQxNDg3OTExLCJuYmYiOjE3Mjk0ODc4NTEsImlhdCI6MTcyOTQ4Nzg1MSwianRpIjoiNTYxNDdkNjEtNTNhYy00MGEzLTllNjgtZDI2MzU4ODUwMDkzIn0.k0VkFRQCcWuBu5o7f8C2NrYPmEua9BTIEXC18V1hJSfCtakQJYFvAa-c1vpuJnzGM2mFv0SBfFKcUJgQiCQzbl2IJUyfSJb-PcCiwpy40JgX3MEDNC9sNNyDaDazjWbBLQXcBND0BERbMKe0Y9H-stOsrg-zCAq1c2H8u1vrCxM")
        .header("X-source", "spark")
        .header("Content-Type", "application/json")
        .body(StringBody(
          session => {
            val currentTimestampMillis = Instant.now().toEpochMilli
            s"""{
               |    "token": {
               |        "token": "324",
               |        "exchange": "NSE",
               |        "type": "EQUITY"
               |    },
               |    "conditionId": 26354996,
               |    "timestampEpochMillis": $currentTimestampMillis,
               |    "meta": {
               |        "patternWidth": 1,
               |        "conditionId": 123,
               |        "description": "Bearsh doji formed on 5min candle"
               |    }
               |}""".stripMargin
          }
        ))
        .check(status is 200)
      )
  }

//Set-up

val scn_CRUD_APIs = scenario("CRUD_APIs")
                    .forever{
//                       exec(Token_API())
                       // exec(Create_Backtest_API())
//                      .exec(GetSchema_API())
                        exec(ConditionFeed_API())
                        .exec(ConditionsFeed_Bulk_API())

                      }

val scn_CALL_BACK = scenario("CALL_BACK")
  .forever{
    //                       exec(Token_API())
    // exec(Create_Backtest_API())
    //                      .exec(GetSchema_API())
    exec(CallBack_Api())

  }

  val httpProtocolMap : scala.collection.mutable.Map[io.gatling.core.structure.ScenarioBuilder, io.gatling.http.protocol.HttpProtocolBuilder] = scala.collection.mutable.Map.empty[io.gatling.core.structure.ScenarioBuilder, io.gatling.http.protocol.HttpProtocolBuilder]
  var ScenariosToRun = new ListBuffer[io.gatling.core.structure.PopulationBuilder]()

  if ((Test_Model.contains("Load_Test")) || (Test_Model.contains("Smoke_Test"))){
    val scnMap : scala.collection.mutable.Map[io.gatling.core.structure.ScenarioBuilder, Int] = scala.collection.mutable.Map.empty[io.gatling.core.structure.ScenarioBuilder, Int]
    val testDuration = InputsForTest(0).toInt

  if(scenariosToExecute.contains("CRUD_APIs")){
      val scn = scn_CRUD_APIs
      val user = InputsForTest(1).toInt      
      scnMap += (scn -> user)
      httpProtocolMap += (scn -> httpconf_coreUrl)
  }

  if (scenariosToExecute.contains("CALL_BACK")) {
    val scn = scn_CALL_BACK
    val user = InputsForTest(1).toInt
    scnMap += (scn -> user)
    httpProtocolMap += (scn -> httpconf_coreUrl)
  }

    ScenariosToRun = InjectionProfile.LoadTestInjectionProfile(scnMap , httpProtocolMap,ScenariosToRun)
    val setupCustom = ScenariosToRun.toList
    setUp(setupCustom).maxDuration(testDuration seconds)
  }

  if (Test_Model.contains("Scalability_Test")){

    val StartWithUsersCount = InputsForTest(0).toInt
    val StepUsersPerSec     = InputsForTest(1).toInt
    val StepRampDuration    = InputsForTest(2).toInt
    val StepDuration        = InputsForTest(3).toInt
    val NoOfSteps           = InputsForTest(4).toInt

    val ScaleInputs : String = List(StartWithUsersCount,StepUsersPerSec,StepRampDuration,StepDuration,NoOfSteps).mkString(",")
    
    val testDuration: Int = if (StartWithUsersCount > 0)
                              ((NoOfSteps-1) * StepRampDuration) + ( NoOfSteps * StepDuration )
                            else  
                               NoOfSteps * ( StepRampDuration + StepDuration )

  if(scenariosToExecute.contains("CRUD_APIs")){
      val scn = scn_CRUD_APIs     
      httpProtocolMap += (scn -> httpconf_coreUrl)
  }  
  
   ScenariosToRun = InjectionProfile.SclabilityTestInjectionProfile(ScaleInputs , httpProtocolMap, ScenariosToRun)
   val setupCustom = ScenariosToRun.toList
   setUp(setupCustom).maxDuration(testDuration seconds)  
  }

  if (Test_Model.contains("TPS_Based_Test")){

    val Users         = InputsForTest(0).toInt
    val TargetTPS     = InputsForTest(1).toInt
    val InitialRamp   = InputsForTest(2).toInt
    val testDuration  = InputsForTest(3).toInt
    val totalDuration = InitialRamp + testDuration

    val TPSInputs : String = List(Users,TargetTPS,InitialRamp,testDuration).mkString(",")

  if(scenariosToExecute.contains("CRUD_APIs")){
      val scn = scn_CRUD_APIs     
      httpProtocolMap += (scn -> httpconf_coreUrl)
  }
   
   ScenariosToRun = InjectionProfile.TPSTestInjectionProfile(TPSInputs , httpProtocolMap, ScenariosToRun)
   val setupCustom = ScenariosToRun.toList
   setUp(setupCustom).maxDuration(totalDuration seconds)
  }

}

