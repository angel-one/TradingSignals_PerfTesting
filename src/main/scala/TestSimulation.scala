import io.gatling.core.Predef._
import io.gatling.http.Predef._

class TestSimulation extends Simulation {

  val httpConf = http.baseUrl("http://yourapi.com")
    .doNotTrackHeader("1")

  val scn = scenario("BasicSimulation")
    .exec(http("request_1")
      .get("/"))
    .pause(5)

  setUp(scn.inject(atOnceUsers(1))).protocols(httpConf)
}