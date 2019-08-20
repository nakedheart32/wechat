package com.abc.wechat.service;

import com.abc.wechat.entity.Contact;

import java.util.List;

public interface ContactsService {
    public List<Contact> selectAllContacts();
}
