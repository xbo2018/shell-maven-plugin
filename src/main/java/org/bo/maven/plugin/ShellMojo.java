package org.bo.maven.plugin;


import com.jcraft.jsch.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.bo.maven.plugin.utils.ConsoleProgressBar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 上传文件到远程服务器并执行指定的shell命令
 * @author zzbinfo@qq.com
 * @version 1.0,2016-09-04
 *@goal exec
 *@requiresProject false
 */
public class ShellMojo extends AbstractMojo {

    /**
     * ssh://user@host:port,password
     * @parameter
     * @requiredz
     */
    private String url;
    /**
     * @parameter
     * @required
     */
    private String[] commands;
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        SshCommandExecutor sshCommandExecutor=parseExecutor(url);
        if(sshCommandExecutor!=null){
            sshCommandExecutor=parseExecutor(url);
            getLog().info("execute at "+url);
            sshCommandExecutor.execute(commands);
        }else {
            throw new MojoExecutionException("not support.url:" + url);
        }
    }
    private SshCommandExecutor parseExecutor(String url) throws MojoExecutionException{
        SshCommandExecutor sshCommandExecutor=null;
        try {
            //if (StringUtils.startsWith(url, "ssh://")) {
                String user = StringUtils.substringBetween(url, "ssh://", "@");
                if(StringUtils.isEmpty(user)){
                    throw new MojoExecutionException("user unknown.user:"+user);
                }
                String host = null;
                if (StringUtils.indexOf(url, ":", 7) > 0) {
                    host = StringUtils.substringBetween(url, "@", ":");
                } else {
                    host = StringUtils.substringBetween(url, "@", ",");
                }
                if(StringUtils.isEmpty(host)){
                    throw new MojoExecutionException("host unknown.host:"+host);
                }
                int port = 0;
                int portIndex = StringUtils.indexOf(url, host) + host.length();
                if (url.charAt(portIndex) == ':') {
                    port = Integer.parseInt(StringUtils.substring(url, portIndex + 1, StringUtils.indexOf(url, ',', portIndex)));
                }
                String password = StringUtils.substring(url, StringUtils.lastIndexOf(url, ",") + 1);
                sshCommandExecutor = new SshCommandExecutor(user, host, password, port, getLog());
            //}
            return sshCommandExecutor;
        }catch (Exception e){
            throw new MojoExecutionException("url is unknown format[ssh://user@host:port,password].url:"+url,e);
        }
    }
    private static class SshCommandExecutor{
        private Log logger;
        private String user,host,password;
        private int port;
        public SshCommandExecutor(String user,String host,String password,int port,Log log){
            this.user=user;
            this.password=password;
            this.host=host;
            this.port=port;
            this.logger=log;
        }
        final String charset = "UTF-8"; // 设置编码格式
        public void execute(String[] commands)throws MojoExecutionException{
            if(commands==null||commands.length==0){
                logger.warn("not found command.");
                return;
            }
            Session session = null;
            JSch jsch = new JSch();
            try {
                if(port <=0){
                    //连接服务器，采用默认端口
                    session = jsch.getSession(user, host);
                }else{
                    //采用指定的端口连接服务器
                    session = jsch.getSession(user, host ,port);
                }
                //设置登陆主机的密码
                session.setPassword(password);//设置密码
                MyUserInfo userInfo = new MyUserInfo();
                session.setUserInfo(userInfo);
                logger.info("connecting......");
                session.connect();
                for(String command:commands) {
                    if(StringUtils.startsWithIgnoreCase(command,"@scp")){
                        uploadFile(session,command);
                    }else {
                        exeCommand(session, command);
                    }
                }
            } catch (Exception e) {
                throw new MojoExecutionException("execute error",e);
            } finally {
                if(session!=null){
                    try {
                        session.disconnect();
                    }catch (Exception e){

                    }
                }
            }
        }
        private void uploadFile(Session session,String command) throws Exception{
            logger.info("upload file:"+command);
            String[] args= StringUtils.split(command," ");
            if(args.length<3){
                logger.warn("command can not be executed,parameter not recognize.[@scp localSrcPath remoteDestinationPath] command:"+command);
                return;
            }
            int start=1;
            String src=parsePath(command,args,start);
            for (int i=args.length-1;i>0;i--){
                if(StringUtils.containsOnly(args[i],src+"\"")){
                    start=i+1;
                    break;
                }
            }
            String destination=parsePath(command,args,start);
            ChannelSftp channel = (ChannelSftp)session.openChannel("sftp"); // 打开SFTP通道
            channel.connect(); // 建立SFTP通道的连接
            channel.put(src, destination, new MyProgressMonitor(), ChannelSftp.OVERWRITE);
            channel.quit();
        }
        private String parsePath(String command,String[] args,int startIndex){
            String path="";
            if(StringUtils.startsWith(args[startIndex],"\"")){
                for(int i=startIndex;i<args.length;i++){
                    if(StringUtils.endsWith(args[i],"\"")){
                        path=StringUtils.substring(command, StringUtils.indexOf(command, args[startIndex])+1, StringUtils.indexOf(command,args[i])+args[i].length()-1);
                        break;
                    }
                }
            }
            if(path==""){
                path=args[startIndex];
            }
            return path;
        }
        private void exeCommand(Session session,String   command) throws Exception{
            logger.info("exe command :" + command);
            // Create and connect channel.
            ChannelExec channel = (ChannelExec)session.openChannel("exec");
            BufferedReader input = new BufferedReader(new InputStreamReader(channel
                    .getInputStream(),charset));
            BufferedReader error = new BufferedReader(new InputStreamReader( ((ChannelExec)channel).getErrStream(),charset));
            ((ChannelExec) channel).setCommand(command);
            channel.connect();
            // Get the output of remote command.
            printReader(input,null);
            StringBuffer sb=new StringBuffer();
            sb.append("exe command :");
            sb.append(command);
            sb.append("\n");
            boolean hasError=printReader(error,sb);
            input.close();
            error.close();
            //when session.disconnect,then channel.disconnect.
            if(hasError){
                throw new MojoExecutionException(sb.toString());
            }
        }
        private boolean printReader(BufferedReader input,StringBuffer sb) throws IOException{
            String line;
            boolean readed=false;
            while ((line = input.readLine()) != null) {
                if(sb!=null){
                    sb.append(line);
                    sb.append("\n");
                }
                System.out.println(line);
                if(!readed){
                    readed=true;
                }
            }
            return readed;
        }
        private static class MyProgressMonitor implements SftpProgressMonitor {
            private long transfered;
            private long max;
            int percentage;
            ConsoleProgressBar cpb = new ConsoleProgressBar(0, 100, 70, '=');;
            @Override
            public boolean count(long count) {
                transfered = transfered + count;
                int newPercentage=(int)((transfered*100)/max);
                if(percentage!=newPercentage) {
                    percentage=newPercentage;
                    /*System.out.print("+++" + percentage + "%");
                    if(percentage<100&&percentage %20 ==0){
                        System.out.println();
                    }*/
                    cpb.show(newPercentage);
                }
                return true;
            }

            @Override
            public void end() {
                //System.out.println();
                System.out.println("Transferring done.");
            }

            @Override
            public void init(int op, String src, String dest, long max) {
                //op - a code indicating the direction of transfer, one of PUT and GET
                //dest - the destination file name.
                //max - the final count (i.e. length of file to transfer).
                System.out.print("Transferring begin.");
                System.out.print("max length:");
                System.out.print(max);
                System.out.print(" bytes");
                System.out.print(",src:");
                System.out.print(src);
                System.out.print(",destination:");
                System.out.print(dest);
                System.out.println();
                System.out.print("progress:");
                this.max=max;
            }
        }
        private static class MyUserInfo implements UserInfo {
            private String password;

            private String passphrase;

            @Override
            public String getPassphrase() {
                 return passphrase;
            }

            @Override
            public String getPassword() {
                return password;
            }

            @Override
            public boolean promptPassphrase(final String promptPassphrase) {
                System.out.println("MyUserInfo.promptPassphrase()");
                System.out.println(promptPassphrase);
                return false;
            }

            @Override
            public boolean promptPassword(final String promptPassword) {
                System.out.println("MyUserInfo.promptPassword()");
                System.out.println(promptPassword);
                return false;
            }

            @Override
            public boolean promptYesNo(final String prompt) {
                if (prompt.contains("The authenticity of host")) {
                    return true;
                }else{
                    System.out.println("MyUserInfo.promptYesNo()");
                    System.out.println(prompt);
                }
                return false;
            }

            @Override
            public void showMessage(final String message) {
                System.out.println("MyUserInfo.showMessage()");
                System.out.println(message);
            }
        }
    }
}