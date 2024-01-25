import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor(private router:Router) { }

  loggedIn(){
    return !!localStorage.getItem('token');
  }
  onLogout(){
    localStorage.removeItem('token');
    alert('Logged out successfully');
    this.router.navigate([''])
  }

}
