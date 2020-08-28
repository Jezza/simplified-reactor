
### Simplified Maven Reactor

For some reason, parent projects in maven aren't allowed to produce artefacts.

I didn't need a complex parent/child construct.  
I just needed to install some sub-projects if they were found.

This extension does exactly that.

Simply add the necessary junk, and it'll all "just work":

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.github.jezza</groupId>
            <artifactId>simplified-reactor-plugin</artifactId>
            <version>0.2</version>
            <extension>true</extension>
            <configuration>
                <!--
                    The plugin will attempt to locate the project
                    relative to the current folder.
                    You can also use absolute paths.
                -->
                <child>../my-internal-project</child>
                <child>../my-other-project</child>
            </configuration>
        </plugin>
    </plugins>
</build>

```

