
### Simplified Maven Reactor

For some reason, parent projects in maven aren't allowed to produce artefacts.

I didn't need a complex parent/child construct.  
I just needed to install some sub-projects if they were found.

This extension does exactly that.

Simply add the necessary junk, and it'll all "just work":

```xml
<properties>
    <!--
    The paths are relative to the project's pom.xml.
    It will also append `pom.xml` if necessary.
    -->
    <reactor.children>
        ../my-internal-project
        ../my-other-project
    </reactor.children>
</properties>

<build>
    <extensions>
        <extension>
            <groupId>com.github.jezza</groupId>
            <artifactId>simplified-reactor-plugin</artifactId>
            <version>0.2</version>
        </extension>
    </extensions>
</build>

```

