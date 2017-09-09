
import _root_.controllers.{Assets, AssetsComponents}
import akka.stream.Materializer
import com.softwaremill.macwire._
import play.api.ApplicationLoader.Context
import play.api._
import play.api.http._
import play.api.i18n._
import play.api.libs.crypto.CSRFTokenSigner
import play.api.mvc._
import play.api.routing.Router
import play.filters.HttpFiltersComponents
import router.Routes

/**
 * Application loader that wires up the application dependencies using Macwire
 */
class GreetingApplicationLoader extends ApplicationLoader {
  def load(context: Context): Application = new GreetingComponents(context).application
}

class GreetingComponents(context: Context) extends BuiltInComponentsFromContext(context)
  with GreetingModule
  with AssetsComponents
  with I18nComponents {

  class RequestScope(val request: RequestHeader)
    extends RequestScopeProxy(this)
    with GreetingModuleRequestScope
    with HttpFiltersComponents {

    lazy val httpErrorHandler: HttpErrorHandler =
      new DefaultHttpErrorHandler(environment, configuration, sourceMapper, Some(router))
    lazy val httpRequestHandler =
      new DefaultHttpRequestHandler(router, httpErrorHandler, httpConfiguration, httpFilters: _*)

    lazy val router: Router = {
      val prefix: String = "/"
      wire[Routes]
    }
  }

  // set up logger
  LoggerConfigurator(context.environment.classLoader).foreach {
    _.configure(context.environment, context.initialConfiguration, Map.empty)
  }

  override lazy val httpErrorHandler: HttpErrorHandler =
    new DefaultHttpErrorHandler(environment, configuration, sourceMapper, router = None)

  override lazy val httpRequestHandler = new HttpRequestHandler {
    def handlerForRequest(request: RequestHeader): (RequestHeader, Handler) = {
      val requestScope = new RequestScope(request)
      requestScope.httpRequestHandler.handlerForRequest(request)
    }
  }

  // router is only used by HttpErrorHandler in global scope, which is redefined above
  override lazy val router: Router = movedToRequestScope
  // httpFilters are only used by httpRequestHandler, which is only used in the request scope
  override lazy val httpFilters: Seq[EssentialFilter] = movedToRequestScope

  @inline private def movedToRequestScope: Nothing = throw new UnsupportedOperationException(
    "This method is not meant to be used. Use the method in request scope."
  )
}

class RequestScopeProxy(delegate: GreetingComponents) {
  // These methods allow accessing outer methods when needed by a trait in the request scope
  def configuration: Configuration = delegate.configuration
  def csrfTokenSigner: CSRFTokenSigner = delegate.csrfTokenSigner
  def httpConfiguration: HttpConfiguration = delegate.httpConfiguration
  implicit def materializer: Materializer = delegate.materializer
  def assets: Assets = delegate.assets
}
