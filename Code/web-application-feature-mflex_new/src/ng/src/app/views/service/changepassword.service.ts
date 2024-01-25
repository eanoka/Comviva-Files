import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ChangepaswwordService {
  private changePasswordApiUrl = 'https://mfsbaastest.grameenphone.com/mobiquitypay/v2/user/auth/change-credential';
  private changeSelfPasswordApiUrl = 'https://mfsbaastest.grameenphone.com/mobiquitypay/v2/ums/user/auth/self-set-auth/confirm';

  constructor(private http: HttpClient) { }

  userID = localStorage.getItem('userID');
  changeCredential(oldPassword: string, newPassword: string, confirmNewPassword:string): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json;charset=UTF-8',
    });

    const requestBody = {
      "requestedBy": "SELF",
      "oldAuthenticationValue": oldPassword,
      "newAuthenticationValue": newPassword,
      "confirmedAuthenticationValue": confirmNewPassword,
      "language": "en",
      "workspaceId": "ADMIN",
      "identifierType": "LOGINID",
      "identifierValue": this.userID
    };

    return this.http.post<any>(this.changePasswordApiUrl, requestBody, { headers });
  }




 
  changeSelfCredential(confirmPassword: string, newPassword: string, resumeServiceRequestId:string): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json;charset=UTF-8',
    });

    const requestBody = {
      "confirmedAuthenticationValue": confirmPassword,
      "language": "en",
      "newAuthenticationValue": newPassword,
      "resumeServiceRequestId": resumeServiceRequestId
    };

    return this.http.post<any>(this.changeSelfPasswordApiUrl, requestBody, { headers });
  }

}
