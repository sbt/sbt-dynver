resolvers += Resolver.bintrayIvyRepo("dwijnand", "sbt-plugins")

          addSbtPlugin("com.dwijnand"    % "sbt-dynver"      % "1.0.0-M1")
          addSbtPlugin("io.get-coursier" % "sbt-coursier"    % "1.0.0-M14")
libraryDependencies += "org.scala-sbt"   % "scripted-plugin" % sbtVersion.value
          addSbtPlugin("me.lessis"       % "bintray-sbt"     % "0.3.0")
