import { Injectable } from '@angular/core';
import { Http, Response, URLSearchParams, BaseRequestOptions } from '@angular/http';

@Injectable()
export class CalendarService {
    
    constructor(private http: Http) {}

    getEvents() {
        return this.http.get('showcase/resources/data/scheduleevents.json')
                    .toPromise()
                    .then(res => <any[]> res.json().data)
                    .then(data => { return data; });
    }
}