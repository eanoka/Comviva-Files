import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ForgotpasswordService {

  private fetchTokenApiUrl = 'https://mfsbaastest.grameenphone.com/mobiquitypay/oauth/token';
  private base64Credentials = 'Q29yZVdlYjphZGF5ZmNTV2NJ';
  
  private forgotPasswordApiUrl = 'https://mfsbaastest.grameenphone.com/mobiquitypay/v2/ums/user/auth/self-set-auth/initiate';

  private validOTPApiUrl = 'https://mfsbaastest.grameenphone.com/mobiquitypay/v2/ums/user/auth/self-set-auth/validate-otp';

  constructor(private http: HttpClient) { }

  getToken(): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': 'Basic ' + this.base64Credentials
    });

    const body = new HttpParams()
      .set('grant_type', 'client_credentials');

    return this.http.post<any>(this.fetchTokenApiUrl, body.toString(), { headers: headers });
  }

  verifyUserID(jwtToken: string, identifierValue: string): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${jwtToken}`
    });

    const requestBody = {
      "requestedBy": "SELF",
      "workspaceId": "ADMIN",
      "identifierType": "LOGINID",
      "identifierValue": identifierValue,
      "language": "en",
      "bearerCode": "WEB",
      "deviceInfo": {
        "appName": "MobiquityPayChannel",
        "appVersion": "10.03.0.01",
        "deviceId": "17cf27d2-871b-42d2-aa4c-2930bed0f5e6",
        "isPublicDevice": "N",
        "model": "Google sdk_gphone_x86",
        "os": "ANDROID"
      }
    };
    

    return this.http.post<any>(this.forgotPasswordApiUrl, requestBody, { headers });
  }

  validateOtp(otp: string, requestId: string): Observable<any> {
    const headers = new HttpHeaders({
      'Accept': 'application/json, text/plain, */*',
      'Accept-Language': 'en-US,en;q=0.9,hi;q=0.8',
      'Content-Type': 'application/json;charset=UTF-8',
      'Nonce': 'b03c046cdf821a64',
    });

    const requestBody = {
      "language": "en",
      "otp": otp,
      "resumeServiceRequestId": requestId
    };

    return this.http.post<any>(this.validOTPApiUrl, requestBody, { headers });
  }
}