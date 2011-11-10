import email, imaplib, os, smtplib, zipfile, uuid, subprocess, shutil,time
from email.mime.text import MIMEText
from subprocess import CalledProcessError

# settings
from settings import login, password
detach_dir = 'files'

print 'NLP course mail wrapper'
print 'I\'ll extract files to ' + str(os.curdir) + '/' + detach_dir + ' folder'

print 'logging in imap'
m = imaplib.IMAP4_SSL("imap.gmail.com")
m.login(login, password)

print 'logging in smtp'
server = smtplib.SMTP('smtp.gmail.com')
#server.set_debuglevel(1)
server.ehlo()
server.starttls()
server.ehlo()
server.login(login, password)

#print m.list()
m.select('submissions')

while True:
    resp, items = m.search(None, 'UNSEEN')
    items = items[0].split() # getting the mails id
    
    for emailid in items:
        resp, data = m.fetch(emailid, "(RFC822)") # fetching the mail, "`(RFC822)`" means "get the whole stuff", but you can ask for headers only, etc
        email_body = data[0][1] # getting the mail content
        mail = email.message_from_string(email_body) # parsing the mail content to get a mail object
    
        new_mail = "["+mail["From"]+"] :" + mail["Subject"]
        print 'processing new mail: ' + new_mail
    
        #Check if any attachments at all
        if mail.get_content_maintype() != 'multipart':              
            print 'the mail does not contain any submission, skipping'  
            continue
    
        # we use walk to create a generator so we can iterate on the parts and forget about the recursive headach
        for part in mail.walk():
            # multipart are just containers, so we skip them
            if part.get_content_maintype() == 'multipart':
                continue
    
            # is this part an attachment ?
            if part.get('Content-Disposition') is None:
                continue
    
            filename = part.get_filename()
            counter = 1
    
            # if there is no filename, we create one with a counter to avoid duplicates
            if not filename:
                #m.store(emailid, '+FLAGS', '\Unseen')  
                #filename = 'noname.zip'
                print 'the mail does contain bad submission, skipping'  
                #break
                continue
    
            att_path = os.path.join(detach_dir, filename)
    
            #Check if its already there
            #if not os.path.isfile(att_path) :
                # finally write the stuff
            fp = open(att_path, 'wb')
            fp.write(part.get_payload(decode=True))
            fp.close()
            
            #extracting file
            tmp_dir = os.path.join(detach_dir, 'zip' + str(uuid.uuid1()))
            print 'extracting files to temp dir ' + tmp_dir
            with zipfile.ZipFile(os.path.join(detach_dir, filename), 'r') as zip:            
                os.mkdir(tmp_dir)
                zip.extractall(tmp_dir)     
                
            shutil.copy('./src/main.py', tmp_dir)   
            wdir = os.getcwd()
            os.chdir(tmp_dir)
            result = 'If you see this text something went wrong'
            try:
                result = str(subprocess.check_output(['timeout', '-s', '9', '20m', 'python', 'main.py']))
            except CalledProcessError as e:
                result = 'Your submission failed with return code: ' + str(e.returncode) 
            print 'submission was tested: ' + result
            os.chdir(wdir)
            print 'removing temp dir'
            shutil.rmtree(tmp_dir)
            
            print 'sending answer with result'
            answer_text = 'your request was processed. \n' + result
            answer = MIMEText(answer_text)
            answer['Subject'] = 'Results for ' + str(emailid)
            answer['From'] = 'NLP Course <' + login + '>'
            answer['To'] = mail['From'] 
            server.sendmail(answer['From'], answer['To'], answer.as_string())
            print 'answer sent'
    time.sleep(2)
                    
server.quit()
print 'SMTP connection closed'
