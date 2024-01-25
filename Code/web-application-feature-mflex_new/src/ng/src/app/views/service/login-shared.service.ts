import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LoginSharedService {
  private loginServiceID = new BehaviorSubject<any>(null);
  serviceID$ = this.loginServiceID.asObservable();

  setServiceID(data: any) {
    this.loginServiceID.next(data);
  }
}
