uuid=""
timeToWait=10
if (($DURATION*60/10 > 20));
then
timeToWait=60
else
timeToWait=20
fi
iteration=1

# echo "Starting Python Script for Scaling Up..."
# ScalingStatus=`python3 InfraScaling.py ${ScaleUp_Config}`
# echo "Scaling Status Code = $ScalingStatus"
# echo "Completed Python Script for Scaling Up..."
# exit 1

cat <<EOF > notify.py
import logging
import os
from slack_sdk import WebClient
from slack_sdk.errors import SlackApiError
from pathlib import Path
from dotenv import load_dotenv
import requests

env_path = Path(".") / ".env"
load_dotenv(dotenv_path=env_path)
message=os.environ['message']
slack_token = os.environ["SLACK_TOKEN"]
client = WebClient(token=slack_token)
channel_id=os.environ["channelName"]
ts=""
url= "http://192.168.249.176:1080/job/perfNotifications/buildWithParameters"
payload= {"USER": "1", "DURATION": "1", "SimulationClass": "sparkBO.sparkBOAPI", "LGCount": "1", "channelName": "testnotification" }
def select():
    if "Test is intiated" in message:
        ts = firstMessage(channel_id,message)
        print("timestamp-",ts)
    elif "trigger" in message:
        trigger()
    else:
        ts=os.environ['ts']
        print("Details:-", message,ts)
        post_message(channel_id,message,ts)

def trigger():
    res = requests.get(url, data=payload, auth=('sreperf', '110abac426a7b03267481e30e973f6bca2'))
def firstMessage(Channel,message):
    try:
        response = client.chat_postMessage(
            channel=Channel,
            text=message,
        )
    except SlackApiError as e:
        # You will get a SlackApiError if "ok" is False
        assert e.response["error"]    # str like 'invalid_auth', 'channel_not_found'
    ts=response.data['ts']
    #     post_message(Channel,message,ts)
    return ts
def post_message(Channel,message,ts):
    try:
        response = client.chat_postMessage(
            channel=Channel,
            text=message,
            thread_ts= ts,
        )
    except SlackApiError as e:
        # You will get a SlackApiError if "ok" is False
        assert e.response["error"]    # str like 'invalid_auth', 'channel_not_found'
select()
EOF

echo "Building the JAR"
status="Idle"
ts=""
sbt clean assembly
echo " JAR build successfully"

echo "Checking the availability of the workers"
cat <<EOF > getWorker.py
import requests
status="idle"
urlDetail = "http://192.168.145.69:8080/gatling/server/info"
status = requests.get(urlDetail, headers = { "Authorization": "Basic Z2F0bGluZzpnYXRsaW5n" } )
jsonResponse=status.json()
totalWorkers=len(jsonResponse)
n=0
count=0
while n < totalWorkers:
    if  jsonResponse[n]["status"] == "Idle":
        count += 1
    if n == totalWorkers-1:
        break
    n += 1
print(count)
EOF
workerCount=$(python getWorker.py 2>&1)
echo "IDLE workers Count=$workerCount , Requested LGCount= ${LGCount}"
if  (( $workerCount >= ${LGCount} ))
then
    echo "The expected workers are available now, The test is proceeding"
else
    echo "Exiting the test as the Gatling has the insufficient workers to start the test. you may please wait until the required workers are available"
    exit 1
fi

/bin/bash dist-gatling-client.sh > distgatling.txt
cat distgatling.txt
uuidLine=$( tail -n 1 distgatling.txt )
export uuidLine

build_URL=$(echo $BUILD_URL  | sed -e "s/8080/1080/g")
export build_URL
cat <<EOF > firstMesage.py
import requests
import os
i=0
slackMessage=""
url = os.environ['build_URL'].rstrip('/')+"/api/json"
resp = requests.get(url, auth=("venkateshr", "118b6d74cba0fcc2065863ee7ca147b88e")).json()
length=len(resp['actions'][0]['parameters'])
while i<length:
  if  "channelName" not in resp['actions'][0]['parameters'][i]['name']:
  	slackMessage=slackMessage+resp['actions'][0]['parameters'][i]['name']+": "+resp['actions'][0]['parameters'][i]['value']+"\n> "
  i += 1
print(slackMessage.rstrip(','))
EOF
jobID=$(python3 firstMesage.py 2>&1)
echo $jobID
m1=$(echo Test is intiated  by $BUILD_USER for $BUILD_URL  | sed -e "s/8080/1080/g" )
message="$m1 "$'\n'"> $jobID"
export message
if [[ ${channelName:+1} ]] ; then
    timeStamp=$(python3 notify.py 2>&1)
    ts=$timeStamp
    export ts
fi
echo $ts

cat <<EOF > status.py
import requests
import json
import time
import os
status="idle"
uuid=os.getenv("uuidLine").split("detail/")[1].split("'")[0]
urlDetail = "http://192.168.145.69:8080/gatling/server/detail/" + uuid
status = requests.get(urlDetail, headers = { "Authorization": "Basic Z2F0bGluZzpnYXRsaW5n" } )
jsonResponse=status.json()
status = jsonResponse['status']
print(status)
EOF
while [ "$status" != "COMPLETED" ]
    do
if [ "$status" == "FAILED" ]
then
echo "Test is failed, Hence Exiting"
message="Test is FAILED- ${JENKINS_URL}"
export message
if [[ ${channelName:+1} ]] ; then
    timeStamp=$(python3 notify.py 2>&1)
fi

exit
else
status=$(python status.py 2>&1)
echo "The current status of the job is- $status"
currentWaitTime=$(( (DURATION *  60 + 120) - (timeToWait * iteration )))
message="Test is in progress. Waiting for $timeToWait seconds - ETA is $currentWaitTime seconds"
iteration=$iteration+1
export message
if [[ ${channelName:+1} ]] ; then
  python3 notify.py
fi

sleep $timeToWait
fi
done
echo "Test is Completed, Now Fetching the report"
message=$(echo Test is Completed, Now Fetching the report for $BUILD_URL | sed -e "s/8080/1080/g")
export message
if [[ ${channelName:+1} ]] ; then
  python3 notify.py
fi


export status
cat <<EOF > report.py
import requests
import json
import time
import os
jobStatus = os.getenv("status")
uuid=os.getenv("uuidLine").split("detail/")[1].split("'")[0]
urlReport = "http://192.168.145.69:8080/gatling/server/report/" + uuid
# Executing the report
payload = "{ 'report' : '/resources/"+ uuid + "/index.html' }"
if jobStatus == "COMPLETED":
  report = requests.post(urlReport, headers = { "Authorization": "Basic Z2F0bGluZzpnYXRsaW5n" , "Content-Type": "application/json"  } , data =  { 'report' : payload } )
  jsonResponse=report.json()
  uuid = jsonResponse['report'].split('/')[2]
  print(uuid)
else:
  print("Invalid")
EOF
jobID=$(python report.py 2>&1)
e=$?
if [ "$e" -eq "0" ]
then
  if [ "$jobID" != "Invalid" ]
  then
  message=$(echo Gatling Report link- http://192.168.145.69:8080/resources/$jobID/index.html)
  export message
  echo " Gatling Report link-: http://192.168.145.69:8080/resources/$jobID/index.html"
    if [[ ${channelName:+1} ]] ; then
      python3 notify.py
    fi
  else
  message="The test is failed. Hence, can not retreive the report"
  export message
  echo "The test is failed. Hence, can not retreive the report"
    if [[ ${channelName:+1} ]] ; then
      python3 notify.py
    fi
  fi
else
  jobID=$(echo $uuidLine | cut -d/ -f2)
  message=$(echo Gatling Report link- http://192.168.145.69:8080/resources/$jobID/index.html)
  export message
  echo "Gatling Report link-: http://192.168.145.69:8080/resources/$jobID/index.html"
  if [[ ${channelName:+1} ]] ; then
    python3 notify.py
  fi
fi


# echo "Starting Python Script for Scaling Down..."  
# ScalingStatus=python3 InfraScaling.py ${ScaleDown_Config}
# echo "Scaling Status Code = $ScalingStatus"
# echo "Completed Python Script for Scaling Down..."

exit 0
