package com.abc.wechat.service.Impl;

import com.abc.wechat.dao.db2.ContactsMapper;
import com.abc.wechat.entity.Contact;
import com.abc.wechat.service.ContactsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactsServiceImpl implements ContactsService {

    @Autowired
    private ContactsMapper contactsMapper;

    @Override
    public List<Contact> selectAllContacts() {
        return contactsMapper.selectAllContacts();
    }
}
