import controllers.GreeterController
import play.api.mvc.RequestHeader
import play.api.i18n.Langs
import play.api.mvc.ControllerComponents
import services.ServicesModule

trait GreetingModule extends ServicesModule {

  import com.softwaremill.macwire._

  def langs: Langs

  def controllerComponents: ControllerComponents

  trait GreetingModuleRequestScope {
    def request: RequestHeader

    lazy val greeterController = new GreeterController(request, greetingService, langs, controllerComponents)
  }
}
