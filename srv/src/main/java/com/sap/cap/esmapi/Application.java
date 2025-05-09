package com.sap.cap.esmapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

//Comment added
@SpringBootApplication
@EnableAspectJAutoProxy
@EnableAsync
public class Application  //Main Class
{

	public static void main(String[] args)
	{
		SpringApplication.run(Application.class, args);
	}

}
