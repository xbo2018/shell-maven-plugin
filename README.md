shell maven plugin <br/>
========================
#���
    һ��maven�����ͨ��ssh�߱����¹��ܣ�
        1���ڷ�������ִ��shell�ű�
            <command>ls -al /data</command>
        2����չcommand����maven�ͻ����ϴ��ļ���������
            <command>@scp localSrcPath remoteDestinationPath</command>
#����Ӧ�ó���
    1�����𿪷�����������
	    �༭pom.xml���plugin�����£�
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
#����
    mvn shell:exec