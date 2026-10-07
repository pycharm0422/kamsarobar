import { useEffect, useState } from 'react';
import { KeyboardAvoidingView, Linking, Modal, Platform, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { common } from '../theme';
import { whatsappLink } from '../utils/links';
import { Button, Field } from './ui';

/** Review / edit the ready-made message, then open WhatsApp with it. */
export default function WhatsAppComposer({ member, initialMessage, onClose }) {
  const [message, setMessage] = useState(initialMessage);
  useEffect(() => setMessage(initialMessage), [initialMessage]);
  if (!member) return null;
  return (
    <Modal visible animationType="slide" onRequestClose={onClose}>
      <SafeAreaView style={common.screen}>
        <KeyboardAvoidingView style={[common.content, { flex: 1 }]} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
          <Text style={common.h2}>Message {member.name}</Text>
          <Text style={[common.muted, { marginBottom: 10 }]}>Edit the message if you like, then open WhatsApp.</Text>
          <Field value={message} onChangeText={setMessage} multiline style={{ flex: 1 }} label="Message" />
          <View style={[common.row, { gap: 10 }]}>
            <Button variant="ghost" title="Cancel" onPress={onClose} />
            <Button variant="whatsapp" title="Open WhatsApp" style={{ flex: 1 }}
              onPress={() => { Linking.openURL(whatsappLink(member.mobile, message)); onClose(); }} />
          </View>
        </KeyboardAvoidingView>
      </SafeAreaView>
    </Modal>
  );
}
