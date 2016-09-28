# Toguru Scala Client

Toguru client for Scala applications

## Status

[![Build Status](https://travis-ci.org/AutoScout24/toguru-scala-client.svg)](https://travis-ci.org/AutoScout24/toguru-scala-client)

[![Coverage Status](https://coveralls.io/repos/github/AutoScout24/toguru-scala-client/badge.svg?branch=master)](https://coveralls.io/github/AutoScout24/toguru-scala-client?branch=master)

[![Download](https://api.bintray.com/packages/autoscout24/maven/toguru-scala-client/images/download.svg) ](https://bintray.com/autoscout24/maven/toguru-scala-client/_latestVersion)

## Setup

Add to your `build.sbt` following resolver with dependency:

```scala
resolvers += Resolver.bintrayRepo("autoscout24", "maven")

libraryDependencies += "com.autoscout24" %% "toguru-scala-client" % "(see version number above)",
```

## Basic usage

To toggle code with this client, you need to perform the following steps.

Define where the toggle activations should be fetched from - e.g. in
the Guice module of your Play application so that it can be injected
wherever you need it.

```scala
import toguru.api

val activations = Activations.fromEndpoint("http://localhost:9000/togglestate")
```

Define a toggle using a toggle id and a default activation condition, e.g.
in an object that contains all your toggles. The activation condition
will be fetched from the activations provider based on the toggle id. The
fallback is used if the activations don't know the toggle id given.

```scala
val toggle = Toggle("my-toggle", default = Condition.Off)
```


For toggling based on client information we need to extract information
about the client (e.g. client id and user agent) from a request. Define
an abstract base controller that describes how to provide this information
from a request. Based on that provider and an activations provider, you
can now define a toggled action that enriches the requests with client
information and toggle activation conditions.

```scala
import play.api.mvc._
import toguru.play.PlaySupport

abstract class ToggledController(activations: Activations.Provider) extends Controller {
  import toguru.play.PlaySupport._

  val client: ClientProvider = { implicit request =>
    ClientInfo(userAgent, localeFromCookieValue("culture"), uuidFromCookieValue("myVisitor"), forcedToggle)
  }

  val ToggledAction = PlaySupport.ToggledAction(client, activations)
}

```


Now you can define your controller with toggled actions and use the 
toggle defined earlier to control which code gets executed:

```scala
class MyController @Inject()(activations: Activations.Provider) extends ToggledController(activations) {

  def myAction = ToggledAction { implicit request =>
    if(toggle.isOn)
      Ok("Toggle is on")
    else
      Ok("Toggle is off")
  }
}
```

If you need to enrich the request yourself or can't use PlaySupport's
ToggledAction, you can create the toggle information yourself:

```scala
class MyControllerWithOwnTogglingInfo @Inject()(activations: Activations.Provider) extends ToggledController(activations) {

  def myAction = Action { request =>
    implicit val toggling = PlaySupport.togglingInfo(request, client, activations)

    if(toggle.isOn)
      Ok("Toggle is on")
    else
      Ok("Toggle is off")
  }
}
```

## Testing toggled code

In your tests, you can also define different activation conditions by using
the TestActivations provider.

```scala
import toguru.test.TestActivations

val testActivations = TestActivations(toggle -> Condition.On)()

val controller = new MyController(testActivations)
```

see the [PlaySupport spec](src/test/scala/toguru/play/PlaySupportSpec) for the full usage example in Play.

## Copyright

Copyright (C) 2016 AutoScout24 GmbH.

Distributed under the MIT License.
