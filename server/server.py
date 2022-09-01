from apscheduler.schedulers.background import BackgroundScheduler
from pytz import utc
import threading
import json
import paho.mqtt.client as mqtt
from dbconnector import database
import datetime
import ast
import re
import time

broker = 'broker.emqx.io'
port = 1883
devices_topic = 'diplomatikiuth/autohome/devices'
app_topic = 'diplomatikiuth/autohome/app'
server_topic = 'diplomatikiuth/autohome/server'
client_id = 'MainServer'
username = 'emqx'
password = 'public'

db=database('database/database.db')


def get_float_from_str(status):
    return float(re.findall(r'\d+\.*\d*', str(status))[0])

def trigger_device(id,on_off,delay):
    if delay>0:
        time.sleep(delay/1000.0)
    data_dic = {"action":"trigger_device","id":id,"trigger":on_off}
    client.publish(devices_topic, payload=str(data_dic))

def check_for_new_job():
    automations = db.execute_query('SELECT * FROM automations')
    conditionsls = db.execute_query('SELECT * FROM conditions')
    for automation in automations:
        target_device = db.execute_query('SELECT * FROM cubes WHERE id="%s"'%(automation['target_device_id']))[0]
        if ast.literal_eval(automation['day_schedule'])[datetime.datetime.today().weekday()] != 1: #check if current day is enabled
            continue
        elif automation['target_state'] == target_device['status']:
            continue
        elif automation["start_time"] and automation["end_time"]:
            s_time = get_float_from_str(automation["start_time"])
            e_time = get_float_from_str(automation["end_time"])
            now = datetime.datetime.now().hour+datetime.datetime.now().minute/100.0
            if not (now>=s_time and now<e_time):
                continue
        elif automation["start_time"]:
            s_time = get_float_from_str(automation["start_time"])
            now = datetime.datetime.now().hour+datetime.datetime.now().minute/100.0
            if now!=s_time:
                continue
            
        conditions = []
        for c in conditionsls:
            if c['father'] == automation['id']:
                conditions.append(c)
        conditions_are_met = True
        #check if all conditions are met
        for c in conditions:
            device = db.execute_query('SELECT * FROM sensors WHERE id="%s"'%(str(c['device_id'])))[0]
            if not device: 
                #sensor not found -> search in cubes
                device = db.execute_query('SELECT * FROM cubes WHERE id=%d'%(c['device_id']))[0]
            device_status = get_float_from_str(device['status'])
            is_condition_ok = False
            if c['op'] == '>' and device_status > c['value'] :
                is_condition_ok = True
            elif c['op'] == '<' and device_status < c['value']:
                is_condition_ok = True
            elif c['op'] == '==' and device_status == c['value']:
                is_condition_ok = True
            #If one condition is not met then exit
            if not is_condition_ok:
                conditions_are_met = False
                break

        if conditions_are_met:
            t = threading.Thread(target=trigger_device, args=(automation['target_device_id'],automation['target_state'],automation['start_delay']))
            t.start()


def publish_msg(topic,msg):
    result = client.publish(topic, msg)

def send_devices_to_app():
    cubes = db.execute_query('SELECT * FROM cubes')
    sensors = db.execute_query('SELECT * FROM sensors')
    devices_dic = {"action":"device_list","cubes":cubes,"sensors":sensors}
    client.publish(app_topic, payload=str(devices_dic))

def send_automations_to_app():
    automations = db.execute_query('SELECT * FROM automations')
    conditions = db.execute_query('SELECT * FROM conditions')
    automations_dic = {"action":"automations_list","automations":automations,"conditions":conditions}
    client.publish(app_topic, payload=str(automations_dic))

def msg_recieved(client, userdata, msg):

    try:
        json_data = json.loads(str(msg.payload.decode('utf-8')))
        action = json_data['action']
        if action == 'get_devices':
            send_devices_to_app()
        elif action == 'get_automations':
            send_automations_to_app()
        elif action == 'add_automation':
            db.execute_query('INSERT INTO automations("name","target_device_id","target_state","start_delay","day_schedule","start_time","end_time") VALUES( "%s","%s",%d,%d,"%s","%s","%s") '%(json_data['name'],json_data['target_id'],json_data['target_state'],json_data['start_delay'],json_data['days'],json_data['start_time'],json_data['end_time']))
            father = db.execute_query('SELECT id FROM automations ORDER BY id DESC LIMIT 1')[0]['id']
            con = json_data['conditions']
            for c in con:
                db.execute_query('INSERT INTO conditions("device_id","op","value","father") VALUES( "%s","%s",%d,%d) '%(c['device_id'],c['op'],c['t_value'],father))
        elif action == 'remove_automation':
            id = json_data['id']
            db.execute_query('DELETE FROM automations WHERE id = %d'%(id))
            send_automations_to_app()
        elif action == 'remove_device':
            id = json_data['id']
            db.execute_query('DELETE FROM cubes WHERE id = "%s"'%(id))
            send_devices_to_app()
        elif action == 'rename_device':
            id = json_data['id']
            new_name = json_data['new_name']
            #update cube or sensor
            if not db.execute_query('UPDATE cubes SET name = "%s" WHERE id = "%s"'%(new_name,id)):
                db.execute_query('UPDATE sensors SET name = "%s" WHERE id = "%s"'%(new_name,id))
        elif action == 'device_adoption':
            id = json_data['id']
            tempure = json_data['sensor_temperature']
            humty = json_data['sensor_humidity']
            light = json_data['sensor_light']
            movement = json_data['sensor_movement']
            db.execute_query('INSERT INTO cubes("id","name","status") VALUES( "%s","%s",%d) '%(id,id,0))
            db.execute_query('INSERT INTO sensors("id","name","status","father") VALUES( "%s","%s","%s","%s") '%(tempure['id'],"Θερμοκρασία",tempure["status"],id))
            db.execute_query('INSERT INTO sensors("id","name","status","father") VALUES( "%s","%s","%s","%s") '%(humty['id'],"Υγρασία",humty["status"],id))
            db.execute_query('INSERT INTO sensors("id","name","status","father") VALUES( "%s","%s","%s","%s") '%(light['id'],"Επίπεδα φωτός",light["status"],id))
            db.execute_query('INSERT INTO sensors("id","name","status","father") VALUES( "%s","%s","%s","%s") '%(movement['id'],"Κίνηση",movement["status"],id))
        elif action == 'device_status_update':
            id = json_data['id']
            status = json_data['status']
            #update cube or sensor
            if not db.execute_query('UPDATE cubes SET status = "%s" WHERE id = "%s"'%(status,id)):
                db.execute_query('UPDATE sensors SET status = "%s" WHERE id = "%s"'%(status,id))
            check_for_new_job()

    except Exception as e:
        print(e)




if __name__ == "__main__":

    #start the job scheduler, check for jobs every minute
    scheduler = BackgroundScheduler()
    scheduler.start()
    scheduler.add_job(check_for_new_job, trigger='cron', second=0, timezone=utc)


    # Set mqtt client
    client = mqtt.Client(client_id)
    client.username_pw_set(username, password)

    # connect and set message reciever
    client.connect(broker, port)
    client.on_message = msg_recieved

    #subscribe to server topic
    client.subscribe(server_topic)

    #keep server running
    client.loop_forever()
