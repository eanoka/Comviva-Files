import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class LoginService {
  private fetchTokenApiUrl = 'https://mfsbaastest.grameenphone.com/mobiquitypay/oauth/token';
  private base64Credentials = 'Q29yZVdlYjphZGF5ZmNTV2NJ';
  
  private fetchWebApiUrl = 'https://mfsbaastest.grameenphone.com/mobiquitypay/ums/v3/user/auth/web/login';

  private validateOTPApiUrl = 'https://mfsbaastest.grameenphone.com/mobiquitypay/ums/v3/user/auth/web/login-confirm';
  private getOTPApiUrl = 'https://mfsbaastest.grameenphone.com/mobiquitypay/getOtp/';

  constructor(private http: HttpClient) { }

  getToken(): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/x-www-form-urlencoded',
      'Authorization': 'Basic ' + this.base64Credentials
    });

    const body = new HttpParams()
      .set('grant_type', 'client_credentials');

 
// this.handleTokenCall()
    return this.http.post<any>(this.fetchTokenApiUrl, body.toString(), { headers: headers ,responseType:"json" });
    // return this.http.post<any>('https://fakestoreapi.com/users', body.toString(), { headers: headers ,responseType:"json" });
  }


  async handleTokenCall(){
    const response = await fetch('https://mfsbaastest.grameenphone.com/mobiquitypay/oauth/token', {
      method: 'POST',
      headers: {
        'Authorization': 'Basic Q29yZVdlYjphZGF5ZmNTV2NJ'
      },
      body: new URLSearchParams({
        'grant_type': 'client_credentials'
      })
    });

  }

  getWeb(jwtToken: string, identifierValue: string, authenticationValue: string): Observable<any> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${jwtToken}`
    });

    const body = {
      "bearerCode": "WEB",
      "language": "en",
      "workspaceId": "ADMIN",
      "identifierType": "LOGINID",
      "identifierValue": identifierValue, // user ID
      "authenticationValue": authenticationValue, // Password
      "isTokenRequired": "Y",
      "deviceInfo": {
        "appName": "mobilePay",
        "appVersion": "V X.9",
        "deviceId": "ffed2d4608c5191f5086b2f2cf160afd",
        "browser": "Google Chrome",
        "isPublicDevice": "N",
        "model": "Desktop - Windows 10",
        "providerIpAddress": "136.226.255.14"
      }
    };

    return this.http.post(this.fetchWebApiUrl, body, { headers: headers });
  }

  validateOTP(otp: string, serviceRequestId: string): Observable<any> {
  
    // const headers = new HttpHeaders({
    //   'Accept': 'application/json, text/plain, */*',
    //   'Accept-Language': 'en-US,en;q=0.9,hi;q=0.8',
    //   'Content-Type': 'application/json;charset=UTF-8',
    //   'Nonce': 'd17f187090ede6ab',
    //   'Origin': 'https://mfsbaastest.grameenphone.com',
    //   'Referer': 'https://mfsbaastest.grameenphone.com/dfscontainer/app.html',
    //   'Sec-Fetch-Dest': 'empty',
    //   'Sec-Fetch-Mode': 'cors',
    //   'Sec-Fetch-Site': 'same-origin',
    //   'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36',
    //   'sec-ch-ua': '"Google Chrome";v="117", "Not;A=Brand";v="8", "Chromium";v="117"',
    //   'sec-ch-ua-mobile': '?0',
    //   'sec-ch-ua-platform': '"Windows"'
    // });
    // { headers: headers }

    const requestBody = {
      "resumeServiceRequestId": serviceRequestId,
      "otp": otp,
      "serviceCode": "LOGIN_POLICY"
    };

    return this.http.post<any>(this.validateOTPApiUrl, requestBody);
  }

  getOtp(type:string,userId:string){
    

    const url = `${this.getOTPApiUrl}${type}/${userId}`

    return this.http.get<any>(url); 
  }
}
