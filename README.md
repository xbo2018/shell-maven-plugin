shell maven plugin <br/>
========================
#简介
    一个maven插件，通过ssh具备以下功能：
        1、在服务器上执行shell脚本
            <command>ls -al /data</command>
        2、扩展command，从maven客户端上传文件到服务器
            <command>@scp localSrcPath remoteDestinationPath</command>
#典型应用场景
    1、部署开发包到服务器
	    编辑pom.xml添加plugin，如下：
        <plugin>
            <groupId>org.bo.maven.plugin</groupId>
            <artifactId>shell-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <configuration>
                <url>ssh://user@host:port,password</url>
                <commands>
                    <command>ls -al /data</command>
                    <command>@scp ${project.build.directory}/${project.build.finalName}.${project.packaging} /data/</command>
                    <command>ls -al /data</command>
                </commands>
            </configuration>
        </plugin>
#构建
    mvn shell:exec