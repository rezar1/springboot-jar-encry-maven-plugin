# springboot-jar-encry-maven-plugin

mvn插件，加密运行springboot jar项目
1. 在maven test阶段后修改编译后的数据，加密相关字节码文件和资源文件；同时在当前目录生成解密文件decry.psd;
2. 覆盖原有SpringBoot启动类，添加自定义加载器，需要当前目录下存在decry.psd文件并且密码正确才能正常启动jar项目

# 引入:

pom.xml文件添加

```
    <build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
			<plugin>
				<groupId>com.mojin.kernel</groupId>
				<artifactId>springboot-class-encry-maven-plugin</artifactId>
				<version>0.0.1-SNAPSHOT</version>
				<configuration>
					<encryItemes>
						<!-- 
							encryItem 包含三类属性:
							1. packageName
								加密[aaaa.bbb.ccc]包名下的所有类和资源文件
							2. javaFile
								加密[aaa.bbb.ddd.AClass]特定类 
							3. normalFile
								加密普通资源文件，
								如src/main/resources/下的
								 a. application.yaml
							   b. jdbc.properties
						 -->
						<encryItem>
							<javaFile>com.yusheng.ssp.applications.WorkCounterApplication</javaFile>
						</encryItem>
						<encryItem>
							<normalFile>application.yaml</normalFile>
						</encryItem>
					</encryItemes>
					<excludeEncryPackagesOrClasses>
						<!-- 忽略掉的包和文件,以 / 分割的包/类 或 包/资源文件，注意需要带文件后缀，并排除正则特殊字符 -->
						<excludeEncryPackagesOrClasse>
							com/yusheng/ssp/RedirectGeneralKernelApplication\.class
						</excludeEncryPackagesOrClasse>
					</excludeEncryPackagesOrClasses>
					<customeClassloaderScanPackages>
						<!-- 需要使用自定义类加载器加载的包, 默认为启动类所在的包 -->
						<customeClassloaderScanPackage>
						</customeClassloaderScanPackage>
					</customeClassloaderScanPackages>
					<!-- 自定义密码加密字符，加密使用Aes加密算法，会从字符中随机获取16个字符组成加解密密码 -->
					<encryPasswordChar>fadfsdfljgaojiebladf</encryPasswordChar>
					<!-- 启动后是否删除decry.psd文件，默认为true -->
					<autoDeletePassword>false</autoDeletePassword>
				</configuration>
				<executions>
					<execution>
						<goals>
							<goal>encry</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
     </build>
```
  
  
pom.xml 添加仓库

   
```
    <repositories>
		<repository>
			<id>Rezar-mvn-repo</id>
			<url>https://github.com/rezar1/springboot-encry-maven-plguin/tree/main/springboot-class-encry-maven-plugin/repo</url>
		</repository>
    </repositories>
```

  
  
项目目录下执行打包命令，即可生成加密后的jar和decry.psd文件;

# 注意！！！！
  为避免内存嗅探，建议在jvm启动时添加[-XX:+DisableAttachMechanism]参数，禁止classdump这类操作(ps:同时会禁止掉jstack,jmap等此类工具，权衡使用)



