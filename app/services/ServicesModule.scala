package services

trait ServicesModule {

  import com.softwaremill.macwire._

  lazy val greetingService: GreetingService = wire[GreetingService]

}
