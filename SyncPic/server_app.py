#coding=utf-8
import threading 
import socket
from time import sleep, ctime 

HOST = ''
PORT = 8111
BUFSIZ = 1024
ADDR = (HOST, PORT)	

ListenThread_SleepTime=4
responseKey = "AAA+1+0+Server"
class TcpDataServerThread(threading.Thread):
	
	def __init__(self, sleepTime):
		global tcpSerSock
		tcpSerSock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		tcpSerSock.bind(ADDR)
		tcpSerSock.listen(1)
		threading.Thread.__init__(self)

	def recevieFile(self,filename):
	    print "start Recevie file!"
	    f = open(filename, 'wb')
	    while(True):
	        data = tcpCliSock.recv(BUFSIZ)
	        if not data:
	            break
	        f.write(data)    
	    f.close()  
	    print "Receive successfully!"

	def decodeMsg(self):
	    while True:
	        data = tcpCliSock.recv(BUFSIZ)
	        print "Received data!"
	        if not data:
	            print "Receive invalid msg"
	            tcpCliSock.close()
	            return
	        else:
	            try:
	                action, filename = data.split(',')     
	                if action=="put":
	                    print "filename: "+filename
	                    self.recevieFile(filename)
	                else:
	                    print "other cmd:"+ action
	            except:
	                print "Error, not correct data"
	                return  	

	def run(self):
		global tcpCliSock, addr
		while True:
		    print 'waiting for connection...'
		    tcpCliSock, addr = tcpSerSock.accept()
		    print '...connected from:', addr
		    self.decodeMsg()                	

class UdpServerThread(threading.Thread):
	
	def __init__(self, sleepTime):
		self.udpPort = 22222
		self.threadSleep = sleepTime

		self.sock=socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
		self.sock.bind(("", self.udpPort))


		threading.Thread.__init__(self)
		
		print "finish init!"

	def run(self):
		while(True):
			data,addr=self.sock.recvfrom(2048)
			if not data:
				break
			print "got data from",addr, ctime()
			print data

			self.sock.sendto(responseKey,(addr[0],addr[1]))
	    
 
def main():
    print 'starting at:', ctime()
        
    ListenCtrlThread = UdpServerThread(ListenThread_SleepTime)
    ListenCtrlThread.start()

    recvTcpDataThread = TcpDataServerThread(2)
    recvTcpDataThread.start()

if __name__ == '__main__': 
    main()