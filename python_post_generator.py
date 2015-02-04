import requests
import json

data = {'result': [1,34,324,234,13,24,3],
        'user_id': [123],
	'query': "?fsf=fdoiisdof?Dfsdf",
       }

print "Posted %s" % data
url = "http://cds-csp-test.cern.ch/searchresult"
token = "rh38949834gfg9f9"
print requests.post(url+"?token="+token, data=json.dumps(data)).content
#print requests.post("http://srv3.fncit.no/obelix/searchresult?token=rh38949834gfg9f9", data=json.dumps(data)).content
#print requests.post("http://localhost:4567/searchresult?token=rh38949834gfg9f9", data=json.dumps(data)).content
