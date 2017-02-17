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

## Basic usage in Scala Play

To toggle code with this client, you need to perform the following steps.

Create a Toguru client - e.g. in the Guice module of your Play
application so that it can be injected wherever you need it. For this,
you need to define
* how toggling-relevant client information (e.g. client id and custom
  attributes like culture) can be extracted from a Play request, and
* the Toguru server where the toggle activation conditions can be fetched from.

```scala
import play.api.mvc._
import toguru.play._
import toguru.play.PlaySupport._

val client: PlayClientProvider = { implicit request =>
  ClientInfo(uuidFromCookieValue("myVisitor"), forcedToggle)
}

val toguru = PlaySupport.toguruClient(client, "http://localhost:9000")
```

Define a toggle using a toggle id and a default activation condition, e.g.
in an object that contains all your toggles. The activation condition
will be fetched from the Toguru server based on the toggle id. The
fallback is used if the server can't be reached or the toggle id isn't
in the server's toggle state response.

```scala
val toggle = Toggle("my-toggle", default = Condition.Off)
```

Now, you can determine the state of your toggle based on the client info
and the toggle activation condition from the Toguru server:

```scala
implicit val toggling = toguru(request)

if(toggle.isOn)
  Ok("Toggle is on")
else
  Ok("Toggle is off")
```

## Play support

The Toguru Scala client offers several convenience utilities, however.
To begin with, you can create a toggled controller based on the Toguru
client that your controllers can extend from:

```scala
import play.api.mvc._
import toguru.play._

abstract class ToggledController(toguru: PlayToguruClient) extends Controller {
  val ToggledAction = PlaySupport.ToggledAction(toguru)
}
```

Now you can define your controller with toggled actions and use the
toggle defined earlier to control which code gets executed:

```scala
class MyController @Inject()(toguru: PlayToguruClient) extends ToggledController(toguru) {

  def myAction = ToggledAction { implicit request =>
    if(toggle.isOn)
      Ok("Toggle is on")
    else
      Ok("Toggle is off")
  }
}
```

the request gets enriched with toggling information that is passed into
the toggle.isOn method.

## Custom request enrichment

If you need to enrich the request yourself or can't use PlaySupport's
ToggledAction, you can either apply the `Toggling` trait to your request:

```scala
class MyRequest[A](toguru: PlayToguruClient, request : Request[A])
        extends WrappedRequest[A](request) with Toggling {

  override val client = toguru.clientProvider(request)

  override val activations = toguru.activationsProvider()
}
```

Alternatively, you can always create the toggle information yourself:

```scala
class MyControllerWithOwnTogglingInfo @Inject()(toguru: PlayToguruClient) extends Controller {

  def myAction = Action { request =>
    implicit val toggling = toguru(request)

    if(toggle.isOn)
      Ok("Toggle is on")
    else
      Ok("Toggle is off")
  }
}
```

## Custom client attributes

`ClientInfo` provides means to enrich it with custom attributes. When Creating
a `ClientInfo.Provider`, attributes can be added by using `withAttribute`.
`PlaySupport` offers `fromCookie` and `fromHeader` methods that allow set
custom attribute from a cookie value or a request header, respectively.
Note that the custom attribute will not be set if the cookie or header is
missing.

For example, setting the custom attribute from a cookie named `culture` can be
done like this:

```
val client: PlayClientProvider = { implicit request =>
  ClientInfo(uuidFromCookieValue("myVisitor"), forcedToggle).withAttribute(fromCookie("culture"))
}
```

By this, the activation of a toggle can be controlled based on the value of
custom attributes.

## Testing toggled code

In your tests, you can also define different activation conditions by using
the TestActivations class.

```scala
import toguru.test.TestActivations

val toguruClient = PlaySupport.testToguruClient(client, TestActivations(toggle -> Condition.On)())

val controller = new MyController(toguruClient)
```

see the [PlaySupport spec](src/test/scala/toguru/play/PlaySupportSpec.scala) for
full usage examples in Scala Play.

## Copyright

Copyright (C) 2017 AutoScout24 GmbH.

Distributed under the MIT License.
